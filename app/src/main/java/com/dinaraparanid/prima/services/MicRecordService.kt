package com.dinaraparanid.prima.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import arrow.core.continuations.update
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.RecorderService
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** [Service] for audio recording */

class MicRecordService : RecorderService() {
    private var timeMeter = AtomicInteger()
    private val recordingExecutor = Executors.newFixedThreadPool(2)
    private var timeMeterTask: Future<*>? = null
    private var recordingTask: Future<*>? = null
    private var timeMeterCondition = AsyncCondVar()

    private var mediaRecord: AtomicReference<MediaRecorder> = AtomicReference()
    private var savePath = Params.NO_PATH
    private lateinit var filename: String

    private inline var isRecording
        get() = (application as MainApplication).isMicRecording
        set(value) { (application as MainApplication).isMicRecording = value }

    override val updateStyle = Statistics::withIncrementedNumberOfRecorded

    internal companion object {
        private const val MIC_RECORDER_CHANNEL_ID = "mic_recorder_channel"
        private const val NOTIFICATION_ID = 104
        private const val ACTION_PAUSE = "pause"
        private const val ACTION_RESUME = "resume"
        private const val ACTION_STOP = "stop"
        internal const val Broadcast_SET_RECORD_BUTTON_IMAGE = "com.dinaraparanid.prima.SetRecordButtonImage"
        internal const val RECORD_BUTTON_IMAGE_ARG = "record_button_image"

        private inline val curDateAsString
            @JvmStatic
            get() = DateFormat.getInstance().format(Date())
    }

    /**
     * Caller for [MicRecordService] to start recording.
     * @param application [MainApplication] itself wrapped in a [WeakReference]
     */

    internal class Caller(private val application: WeakReference<MainApplication>) {
        @SuppressLint("SyntheticAccessor")
        private var filename = curDateAsString

        internal fun setFileName(filename: String): Caller {
            this.filename = filename
            return this
        }

        private inline val Intent.withExtra
            get() = apply { putExtra(MainActivity.FILE_NAME_ARG, filename) }

        /**
         * Calls of [MicRecordService] to start recording.
         * If it wasn't previously used, connection will be set and recording will start.
         * If it's already connected, recording will start.
         */

        internal fun call() {
            when {
                !application.unchecked.isMicRecordingServiceBounded ->
                    application.unchecked.applicationContext.let { context ->
                        val recorderIntent = Intent(context, MicRecordService::class.java).withExtra

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            context.startForegroundService(recorderIntent)
                        else
                            context.startService(recorderIntent)

                        context.bindService(
                            recorderIntent,
                            application.unchecked.micRecordServiceConnection,
                            BIND_AUTO_CREATE
                        )
                    }

                else -> application.unchecked.sendBroadcast(
                    Intent(MainActivity.Broadcast_MIC_START_RECORDING).withExtra
                )
            }
        }
    }

    private val startRecordingReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnWorkerThread {
                filename = getNewMP3FileNameAsync(
                    intent!!.getStringExtra(MainActivity.FILE_NAME_ARG)!!.correctFileName
                ).await()

                savePath = "${Params.getInstanceSynchronized().pathToSave}/$filename.mp3"
                startRecording(isLocking = true)
            }
        }
    }

    private val stopRecordingReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(context: Context?, intent: Intent?) {
            isRecording = false
            runOnWorkerThread { stopRecording(isLocking = true) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerStartRecordingReceiver()
        registerStopRecordingReceiver()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        runOnWorkerThread { handleIncomingActions(intent, isLocking = true) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(startRecordingReceiver)
        unregisterReceiver(stopRecordingReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel() = (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
        .createNotificationChannel(
            NotificationChannel(
                MIC_RECORDER_CHANNEL_ID,
                "Mic Recorder Service channel",
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_DEFAULT
                    else -> 0
                }
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )

    override suspend fun handleIncomingActionsNoLock(action: Intent?) = when (action!!.action) {
        ACTION_PAUSE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                pauseRecording(isLocking = false)
            isRecording = false
        }

        ACTION_RESUME -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                resumeRecording(isLocking = false)
            isRecording = true
        }

        ACTION_STOP -> {
            isRecording = false
            stopRecording(isLocking = false)
        }

        else -> {
            filename = getNewMP3FileNameAsync(
                action.getStringExtra(MainActivity.FILE_NAME_ARG)!!.correctFileName
            ).await()

            savePath = "${Params.getInstanceSynchronized().pathToSave}/$filename.mp3"
            startRecording(isLocking = false)
        }
    }

    /** Counts seconds during recording */
    private fun startTimeMeterTask() {
        timeMeterTask = recordingExecutor.submit {
            while (isRecording) runOnWorkerThread {
                timeMeterCondition.blockAsync(1000)

                if (isRecording) {
                    timeMeter.incrementAndGet()
                    runOnWorkerThread { buildNotification(isLocking = true) }
                }
            }
        }
    }

    /** Starts recording without any lock */
    private suspend fun startRecordingNoLock() {
        mediaRecord = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> MediaRecorder(applicationContext)
            else -> MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(savePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
            start()
        }.let(::AtomicReference)

        isRecording = true
        startTimeMeterTask()
        buildNotification(isLocking = false)
    }

    private suspend fun startRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { startRecordingNoLock() }
        else -> startRecordingNoLock()
    }

    /**
     * Pauses recording and updates
     * notification without any synchronization
     */

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun pauseRecordingNoLock() {
        if (mediaRecord.get() != null) {
            isRecording = false
            timeMeterCondition.openAsync()

            mediaRecord.update {
                it.pause()
                it
            }

            recordingTask?.get(); recordingTask = null
            timeMeterTask?.get(); timeMeterTask = null
            buildNotification(isLocking = false)
        }
    }

    /** Pauses recording and updates notification */
    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun pauseRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { pauseRecordingNoLock() }
        else -> pauseRecordingNoLock()
    }

    /**
     * Continues recording and updates
     * notification without any synchronization
     */

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun resumeRecordingNoLock() {
        mediaRecord.get().resume()
        isRecording = true
        startTimeMeterTask()
        buildNotification(isLocking = false)
    }

    /** Continues recording and updates notification */
    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun resumeRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { resumeRecordingNoLock() }
        else -> resumeRecordingNoLock()
    }

    /**
     * Finishes recording and removes
     * notification without any synchronization
     */

    private suspend fun stopRecordingNoLock() {
        if (mediaRecord.get() != null) {
            isRecording = false
            timeMeterCondition.openAsync()
            updateStatisticsAsync()

            mediaRecord.update {
                it.stop()
                it.release()
                it
            }

            mediaRecord.set(null)
            recordingTask?.get(); recordingTask = null
            timeMeterTask?.get(); timeMeterTask = null

            try {
                Params.getInstanceSynchronized().application.unchecked.contentResolver.insert(
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Audio.Media.getContentUriForPath(savePath)!!
                    },
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DATA, savePath)
                        put(MediaStore.MediaColumns.TITLE, filename)
                        put(MediaStore.Audio.Media.DURATION, timeMeter.get() * 1000L)
                        put(MediaStore.Audio.Media.IS_MUSIC, true)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Params.getInstanceSynchronized().pathToSave)
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.mp3")
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                    }
                )
            } catch (ignored: Exception) {
                // Some errors with directories
            }

            (application as MainApplication).scanSingleFile(savePath)

            timeMeter.set(0)
            removeNotificationAsync(isLocking = false)
            sendBroadcast(
                Intent(Broadcast_SET_RECORD_BUTTON_IMAGE)
                    .apply { putExtra(RECORD_BUTTON_IMAGE_ARG, false) }
            )
        }
    }

    /** Finishes recording and removes notification */
    private suspend fun stopRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { stopRecordingNoLock() }
        else -> stopRecordingNoLock()
    }

    private fun registerStartRecordingReceiver() = registerReceiver(
        startRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_MIC_START_RECORDING)
    )

    private fun registerStopRecordingReceiver() = registerReceiver(
        stopRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_MIC_STOP_RECORDING)
    )

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotificationNoLock() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> startForeground(
            NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, MIC_RECORDER_CHANNEL_ID)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.octopus)
                .setContentTitle(resources.getString(R.string.record_audio))
                .setContentText("${resources.getString(R.string.recording_time)}: $timeMeter ${resources.getString(R.string.seconds)}")
                .setAutoCancel(true)
                .setSilent(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) addAction(
                        when {
                            isRecording -> NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.pause_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_PAUSE
                                    PendingIntent.getService(
                                        this@MicRecordService,
                                        0,
                                        it,
                                        PendingIntent.FLAG_IMMUTABLE
                                    )
                                }
                            )

                            else -> NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.resume_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_RESUME
                                    PendingIntent.getService(
                                        this@MicRecordService,
                                        1,
                                        it,
                                        PendingIntent.FLAG_IMMUTABLE
                                    )
                                }
                            )
                        }.build()
                    )
                }
                .addAction(
                    NotificationCompat.Action.Builder(
                        null,
                        resources.getString(R.string.stop_recording),
                        Intent(this@MicRecordService, MicRecordService::class.java).let {
                            it.action = ACTION_STOP
                            PendingIntent.getService(
                                this@MicRecordService,
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

        else -> startForeground(
            NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, MIC_RECORDER_CHANNEL_ID)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.octopus)
                .setContentTitle(resources.getString(R.string.record_audio))
                .setContentText("${resources.getString(R.string.recording_time)}: $timeMeter ${resources.getString(R.string.seconds)}")
                .setAutoCancel(true)
                .setSilent(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) when {
                        isRecording -> addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.pause_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_PAUSE
                                    PendingIntent.getService(
                                        this@MicRecordService,
                                        0,
                                        it,
                                        PendingIntent.FLAG_IMMUTABLE
                                    )
                                }
                            ).build()
                        )

                        else -> addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.resume_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_RESUME
                                    PendingIntent.getService(
                                        this@MicRecordService,
                                        1,
                                        it,
                                        PendingIntent.FLAG_IMMUTABLE
                                    )
                                }
                            ).build()
                        )
                    }
                }
                .addAction(
                    NotificationCompat.Action.Builder(
                        null,
                        resources.getString(R.string.stop_recording),
                        Intent(this@MicRecordService, MicRecordService::class.java).let {
                            it.action = ACTION_STOP
                            PendingIntent.getService(
                                this@MicRecordService,
                                0,
                                it,
                                PendingIntent.FLAG_IMMUTABLE
                            )
                        }
                    ).build()
                )
                .build()
        )
    }

    private suspend fun buildNotification(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock() }
        else -> buildNotificationNoLock()
    }
}