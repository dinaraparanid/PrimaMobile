package com.dinaraparanid.prima.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.media.MediaRecorder
import android.os.Build
import android.provider.MediaStore
import arrow.continuations.generic.update
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import com.dinaraparanid.prima.utils.polymorphism.StatisticsUpdatable
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import kotlinx.coroutines.sync.withLock

/** [Service] for audio recording */

class MicRecordService : AbstractService(), StatisticsUpdatable {
    private var timeMeter = 0
    private val recordingExecutor = Executors.newFixedThreadPool(2)
    private var timeMeterTask: Future<*>? = null
    private var recordingTask: Future<*>? = null
    private var timeMeterLock = ReentrantLock()
    private var timeMeterCondition = timeMeterLock.newCondition()

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
        internal const val Broadcast_SET_RECORD_BUTTON_IMAGE =
            "com.dinaraparanid.prima.SetRecordButtonImage"
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
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnWorkerThread { startRecording(isLocking = true) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private val pauseRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            isRecording = false
            runOnWorkerThread { pauseRecording(isLocking = true) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private val resumeRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            runOnWorkerThread { resumeRecording(isLocking = true) }
        }
    }

    private val stopRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isRecording = false
            runOnWorkerThread { stopRecording(isLocking = true) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerStartRecordingReceiver()
        registerStopRecordingReceiver()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerPauseRecordingReceiver()
            registerResumeRecordingReceiver()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        runOnWorkerThread { handleIncomingActions(intent, isLocking = true) }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(startRecordingReceiver)
        unregisterReceiver(stopRecordingReceiver)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(pauseRecordingReceiver)
            unregisterReceiver(resumeRecordingReceiver)
        }
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
            filename = action.getStringExtra(MainActivity.FILE_NAME_ARG)!!.correctFileName
            savePath = "${Params.getInstanceSynchronized().pathToSave}/$filename.mp3"
            startRecording(isLocking = false)
        }
    }

    private fun startTimeMeterTask() {
        timeMeterTask = recordingExecutor.submit {
            while (isRecording) timeMeterLock.withLock {
                timeMeterCondition.await(1, TimeUnit.SECONDS)

                if (isRecording) {
                    timeMeter++
                    runOnWorkerThread { buildNotification(isLocking = true) }
                }
            }
        }
    }

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

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun pauseRecordingNoLock() {
        if (mediaRecord.get() != null) {
            isRecording = false
            timeMeterLock.withLock(timeMeterCondition::signal)

            mediaRecord.update {
                it.pause()
                it
            }

            recordingTask?.get(); recordingTask = null
            timeMeterTask?.get(); timeMeterTask = null
            buildNotification(isLocking = false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun pauseRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { pauseRecordingNoLock() }
        else -> pauseRecordingNoLock()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun resumeRecordingNoLock() {
        mediaRecord.get().resume()
        isRecording = true
        startTimeMeterTask()
        buildNotification(isLocking = false)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun resumeRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { resumeRecordingNoLock() }
        else -> resumeRecordingNoLock()
    }

    private suspend fun stopRecordingNoLock() {
        if (mediaRecord.get() != null) {
            isRecording = false
            timeMeterLock.withLock(timeMeterCondition::signal)
            updateStatisticsAsync()

            mediaRecord.update {
                it.stop()
                it.release()
                it
            }

            mediaRecord.set(null)
            recordingTask?.get(); recordingTask = null
            timeMeterTask?.get(); timeMeterTask = null

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

            timeMeter = 0
            removeNotification(isLocking = false)
            sendBroadcast(
                Intent(Broadcast_SET_RECORD_BUTTON_IMAGE)
                    .apply { putExtra(RECORD_BUTTON_IMAGE_ARG, false) }
            )
        }
    }

    private suspend fun stopRecording(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { stopRecordingNoLock() }
        else -> stopRecordingNoLock()
    }

    private fun registerStartRecordingReceiver() = registerReceiver(
        startRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_MIC_START_RECORDING)
    )

    @RequiresApi(Build.VERSION_CODES.N)
    private fun registerPauseRecordingReceiver() = registerReceiver(
        pauseRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_MIC_PAUSE_RECORDING)
    )

    @RequiresApi(Build.VERSION_CODES.N)
    private fun registerResumeRecordingReceiver() = registerReceiver(
        resumeRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_MIC_RESUME_RECORDING)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) when {
                        isRecording -> addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.pause_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_PAUSE
                                    PendingIntent.getService(this@MicRecordService, 0, it, 0)
                                }
                            ).build()
                        )

                        else -> addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.resume_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_RESUME
                                    PendingIntent.getService(this@MicRecordService, 1, it, 0)
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
                            PendingIntent.getService(this@MicRecordService, 2, it, 0)
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
                                    PendingIntent.getService(this@MicRecordService, 0, it, 0)
                                }
                            ).build()
                        )

                        else -> addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                resources.getString(R.string.resume_recording),
                                Intent(this@MicRecordService, MicRecordService::class.java).let {
                                    it.action = ACTION_RESUME
                                    PendingIntent.getService(this@MicRecordService, 1, it, 0)
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
                            PendingIntent.getService(this@MicRecordService, 0, it, 0)
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