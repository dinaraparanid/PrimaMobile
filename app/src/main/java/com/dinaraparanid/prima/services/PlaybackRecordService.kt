package com.dinaraparanid.prima.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.RecorderService
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import com.vmadalin.easypermissions.EasyPermissions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import linc.com.pcmdecoder.PCMDecoder
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Service for recording app's playback */

@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackRecordService : RecorderService() {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var savePath = Params.NO_PATH
    private lateinit var filename: String

    private var timeMeter = 0
    private val recordingExecutor = Executors.newFixedThreadPool(2)
    private var timeMeterTask: Future<*>? = null
    private var recordingTask: Future<*>? = null
    private var timeMeterCondition = AsyncCondVar()

    private inline var isRecording
        get() = (application as MainApplication).isPlaybackRecording
        set(value) { (application as MainApplication).isPlaybackRecording = value }

    override val updateStyle = Statistics::withIncrementedNumberOfRecorded

    internal companion object {
        private const val PLAYBACK_RECORDER_CHANNEL_ID = "mic_recorder_channel"
        private const val NOTIFICATION_ID = 105
        private const val ACTION_PAUSE = "pause"
        private const val ACTION_RESUME = "resume"
        private const val ACTION_STOP = "stop"
        private const val NUM_SAMPLES_PER_READ = 1024
        private const val BYTES_PER_SAMPLE = 2
        private const val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE
        internal const val EXTRA_RESULT_DATA = "PlaybackRecordService:Extra:ResultData"

        private inline val curDateAsString
            @JvmStatic
            get() = DateFormat.getInstance().format(Date())

        @JvmStatic
        private fun ShortArray.toByteArray(): ByteArray {
            val bytes = ByteArray(size * 2)

            indices.forEach {
                bytes[it * 2] = (get(it).toInt() and 0x00FF).toByte()
                bytes[it * 2 + 1] = (get(it).toInt() shr 8).toByte()
                set(it, 0)
            }

            return bytes
        }
    }

    /**
     * Caller for [PlaybackRecordService] to start recording.
     * @param application [MainApplication] itself wrapped in a [WeakReference]
     */

    internal class Caller(private val application: WeakReference<MainApplication>) {
        @SuppressLint("SyntheticAccessor")
        private var filename = curDateAsString
        private var extraResultData: Intent? = null

        internal fun setFileName(filename: String): Caller {
            this.filename = filename
            return this
        }

        internal fun setExtraData(extraResultData: Intent?): Caller {
            if (extraResultData != null)
                this.extraResultData = extraResultData
            return this
        }

        private inline val Intent.withExtra
            get() = apply {
                putExtra(MainActivity.FILE_NAME_ARG, filename)
                putExtra(EXTRA_RESULT_DATA, extraResultData)
            }

        /**
         * Calls of [PlaybackRecordService] to start recording.
         * If it wasn't previously used, connection will be set and recording will start.
         * If it's already connected, recording will start.
         */

        internal fun call() {
            when {
                !application.unchecked.isPlaybackRecordingServiceBounded ->
                    application.unchecked.applicationContext.let { context ->
                        val recorderIntent = Intent(
                            context,
                            PlaybackRecordService::class.java
                        ).withExtra

                        context.startForegroundService(recorderIntent)

                        context.bindService(
                            recorderIntent,
                            application.unchecked.playbackRecordServiceConnection,
                            BIND_AUTO_CREATE
                        )
                    }

                else -> application.unchecked.sendBroadcast(
                    Intent(MainActivity.Broadcast_PLAYBACK_START_RECORDING).withExtra
                )
            }
        }
    }

    private val startRecordingReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnWorkerThread {
                isRecording = true
                startAudioCapture(intent!!, isLocking = true)
            }
        }
    }

    private val stopRecordingReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnWorkerThread {
                isRecording = false
                stopAudioCapture(isLocking = true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerStartRecordingReceiver()
        registerStopRecordingReceiver()
        mediaProjectionManager = applicationContext
            .getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(startRecordingReceiver)
        unregisterReceiver(stopRecordingReceiver)
    }

    override fun createChannel() = (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
        .createNotificationChannel(
            NotificationChannel(
                PLAYBACK_RECORDER_CHANNEL_ID,
                "Playback Recorder Service channel",
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_DEFAULT
                    else -> 0
                }
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        runOnWorkerThread { handleIncomingActions(intent, isLocking = true) }
        return START_NOT_STICKY
    }

    override suspend fun handleIncomingActionsNoLock(action: Intent?) {
        if (action == null)
            return

        when (action.action) {
            ACTION_PAUSE -> {
                isRecording = false
                pauseRecording(isLocking = false)
            }

            ACTION_RESUME -> {
                isRecording = true
                launchRecording(isLocking = false)
            }

            ACTION_STOP -> {
                isRecording = false
                stopAudioCapture(isLocking = false)
            }

            else -> {
                isRecording = true
                startAudioCapture(action, isLocking = false)
            }
        }
    }

    /** Starts recording without any synchronization */
    private suspend fun startAudioCaptureNoLock(action: Intent) {
        buildNotification(isLocking = false)

        mediaProjection = mediaProjectionManager!!.getMediaProjection(
            Activity.RESULT_OK,
            (action.getParcelableExtra<Parcelable>(EXTRA_RESULT_DATA) as Intent?)!!
        )

        filename = getNewMP3FileNameAsync(
            action.getStringExtra(MainActivity.FILE_NAME_ARG)!!.correctFileName
        ).await()

        savePath = "${Params.getInstanceSynchronized().pathToSave}/$filename.mp3"

        if (ActivityCompat.checkSelfPermission(
                this@PlaybackRecordService,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Unreachable, but linter requires it
            return
        }

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
                .setAudioPlaybackCaptureConfig(
                    AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .build()
                .also(AudioRecord::startRecording)
        } catch (e: UnsupportedOperationException) {
            runOnUIThread {
                Toast.makeText(applicationContext, R.string.not_supported, Toast.LENGTH_LONG).show()
            }

            removeNotificationAsync(isLocking = false)
            releaseAllTasksAndRecorder()
            return
        }

        sendBroadcast(
            Intent(MicRecordService.Broadcast_SET_RECORD_BUTTON_IMAGE)
                .apply { putExtra(MicRecordService.RECORD_BUTTON_IMAGE_ARG, true) }
        )

        launchRecording(isLocking = false)
    }

    /** Starts recording */
    private suspend fun startAudioCapture(action: Intent, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { startAudioCaptureNoLock(action) }
        else -> startAudioCaptureNoLock(action)
    }

    /** Launches recording process without any synchronization */
    private suspend fun launchRecordingNoLock() {
        buildNotification(isLocking = false)

        audioRecord!!.startRecording()
        recordingTask = recordingExecutor.submit { runBlocking { writeAudioToFile(filename) } }

        timeMeterTask = recordingExecutor.submit {
            while (isRecording) runOnWorkerThread {
                timeMeterCondition.blockAsync(1000)

                if (isRecording) {
                    timeMeter++
                    runOnWorkerThread { buildNotification(isLocking = true) }
                }
            }
        }
    }

    /** Launches recording process */
    private suspend fun launchRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { launchRecordingNoLock() }
        else -> launchRecordingNoLock()
    }

    /** Pauses playback's recording without any synchronization */
    private suspend fun pauseRecordingNoLock() {
        if (mediaProjection == null)
            return

        recordingTask?.get(); recordingTask = null
        timeMeterTask?.get(); timeMeterTask = null
        audioRecord!!.stop()
        buildNotification(isLocking = false)
    }

    /** Pauses playback's recording */
    private suspend fun pauseRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { pauseRecordingNoLock() }
        else -> pauseRecordingNoLock()
    }

    /** Saves data and creates new file */
    @SuppressLint("SyntheticAccessor")
    private suspend fun writeAudioToFile(outputFile: String) = FileOutputStream(
        File("${Params.getInstanceSynchronized().pathToSave}/$outputFile.pcm"), true
    ).use {
        val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)

        while (isRecording) {
            audioRecord!!.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
            it.write(capturedAudioSamples.toByteArray(), 0, BUFFER_SIZE_IN_BYTES)
        }
    }

    private fun releaseAllTasksAndRecorder() {
        recordingTask?.get(); recordingTask = null
        timeMeterTask?.get(); timeMeterTask = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    /**
     * Stops audio capture and saves file
     * without any synchronization
     */

    private suspend fun stopAudioCaptureNoLock() {
        if (mediaProjection == null)
            return

        releaseAllTasksAndRecorder()

        recordingExecutor.execute {
            runBlocking {
                PCMDecoder.encodeToMp3(
                    "${Params.getInstanceSynchronized().pathToSave}/$filename.pcm",
                    2,
                    128000,
                    22000,
                    "${Params.getInstanceSynchronized().pathToSave}/$filename.mp3"
                )

                Params.getInstanceSynchronized().application.unchecked.contentResolver.insert(
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Audio.Media.getContentUriForPath(savePath)!!
                    },
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DATA, savePath)
                        put(MediaStore.MediaColumns.TITLE, filename)
                        put(MediaStore.Audio.Media.DURATION, timeMeter * 1000L)
                        put(MediaStore.Audio.Media.IS_MUSIC, true)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Params.getInstanceSynchronized().pathToSave)
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.mp3")
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                    }
                )

                (application as MainApplication).scanSingleFile(savePath)

                timeMeter = 0
                updateStatisticsAsync()
                removeNotificationAsync(isLocking = false)
                sendBroadcast(
                    Intent(MicRecordService.Broadcast_SET_RECORD_BUTTON_IMAGE)
                        .apply { putExtra(MicRecordService.RECORD_BUTTON_IMAGE_ARG, false) }
                )
            }
        }
    }

    /** Stops audio capture and saves file */
    private suspend fun stopAudioCapture(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { stopAudioCaptureNoLock() }
        else -> stopAudioCaptureNoLock()
    }

    private fun registerStartRecordingReceiver() = registerReceiver(
        startRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_PLAYBACK_START_RECORDING)
    )

    private fun registerStopRecordingReceiver() = registerReceiver(
        stopRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_PLAYBACK_STOP_RECORDING)
    )

    private fun buildNotificationNoLockUnchecked() = startForeground(
        NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, PLAYBACK_RECORDER_CHANNEL_ID)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.octopus)
            .setContentTitle(resources.getString(R.string.record_audio))
            .setContentText("${resources.getString(R.string.recording_time)}: $timeMeter ${resources.getString(
                R.string.seconds)}")
            .setAutoCancel(true)
            .setSilent(true)
            .addAction(
                when {
                    isRecording -> NotificationCompat.Action.Builder(
                        null,
                        resources.getString(R.string.pause_recording),
                        Intent(this, PlaybackRecordService::class.java).let {
                            it.action = ACTION_PAUSE
                            PendingIntent.getService(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
                        }
                    )

                    else -> NotificationCompat.Action.Builder(
                        null,
                        resources.getString(R.string.resume_recording),
                        Intent(this, PlaybackRecordService::class.java).let {
                            it.action = ACTION_RESUME
                            PendingIntent.getService(this, 1, it, PendingIntent.FLAG_IMMUTABLE)
                        }
                    )
                }.build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    resources.getString(R.string.stop_recording),
                    Intent(this@PlaybackRecordService, PlaybackRecordService::class.java).let {
                        it.action = ACTION_STOP
                        PendingIntent.getService(
                            this@PlaybackRecordService,
                            2,
                            it,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                ).build()
            )
            .build(),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    )

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotificationNoLock() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                if (EasyPermissions.hasPermissions(
                        applicationContext, Manifest.permission.POST_NOTIFICATIONS
                    )
                ) buildNotificationNoLockUnchecked()

            else -> buildNotificationNoLockUnchecked()
        }
    }

    private suspend fun buildNotification(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock() }
        else -> buildNotificationNoLock()
    }
}