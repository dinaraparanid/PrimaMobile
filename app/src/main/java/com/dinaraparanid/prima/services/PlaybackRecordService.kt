package com.dinaraparanid.prima.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import linc.com.pcmdecoder.PCMDecoder
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Service for recording app's playback
 */

@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackRecordService : AbstractService() {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var savePath = Params.NO_PATH
    private lateinit var filename: String

    private var timeMeter = 0
    private val recordingExecutor = Executors.newFixedThreadPool(2)
    private var timeMeterCoroutine: Future<*>? = null
    private var recordingCoroutine: Future<*>? = null
    private var timeMeterLock = ReentrantLock()
    private var timeMeterCondition = timeMeterLock.newCondition()

    private inline var isRecording
        get() = (application as MainApplication).isPlaybackRecording
        set(value) { (application as MainApplication).isPlaybackRecording = value }

    internal companion object {
        private const val PLAYBACK_RECORDER_CHANNEL_ID = "mic_recorder_channel"
        private const val NOTIFICATION_ID = 105
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

            (indices).forEach {
                bytes[it * 2] = (get(it).toInt() and  0x00FF).toByte()
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
        private var filename = curDateAsString

        internal fun setFileName(filename: String): Caller {
            this.filename = filename
            return this
        }

        private inline val Intent.withExtra
            get() = apply { putExtra(MainActivity.FILE_NAME_ARG, filename) }

        /**
         * Calls of [PlaybackRecordService] to start recording.
         * If it wasn't previously used, connection will be set and recording will start.
         * If it's already connected, recording will start.
         */

        internal fun call() {
            when {
                !application.unchecked.isPlaybackRecordingServiceBounded ->
                    application.unchecked.applicationContext.let { context ->
                        val recorderIntent = Intent(context, PlaybackRecordService::class.java).withExtra
                        context.startForegroundService(recorderIntent)

                        context.bindService(
                            recorderIntent,
                            application.unchecked.playbackRecordServiceConnection,
                            BIND_AUTO_CREATE
                        )
                    }

                else -> application.unchecked.sendBroadcast(
                    Intent(MainActivity.Broadcast_MIC_START_RECORDING).withExtra
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = applicationContext
            .getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createChannel()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun handleIncomingActions(action: Intent?) = when (action!!.action) {
        ACTION_STOP -> {
            isRecording = false
            stopAudioCapture()
        }

        else -> {
            mediaProjection = mediaProjectionManager!!.getMediaProjection(
                Activity.RESULT_OK,
                (action.getParcelableExtra<Parcelable>(EXTRA_RESULT_DATA) as Intent?)!!
            )

            filename = action.getStringExtra(MainActivity.FILE_NAME_ARG)!!.correctFileName
            savePath = "${Params.instance.pathToSave}/$filename.mp3"
            startAudioCapture()
        }
    }

    @Synchronized
    private fun startAudioCapture() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

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

        isRecording = true
        buildNotification()

        recordingCoroutine = recordingExecutor.submit { writeAudioToFile(File(filename)) }

        timeMeterCoroutine = recordingExecutor.submit {
            while (isRecording) timeMeterLock.withLock {
                timeMeterCondition.await(1, TimeUnit.SECONDS)

                if (isRecording) {
                    timeMeter++
                    buildNotification()
                }
            }
        }
    }

    private fun writeAudioToFile(outputFile: File) = FileOutputStream(outputFile).use {
        val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)

        while (isRecording) {
            audioRecord!!.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
            it.write(capturedAudioSamples.toByteArray(), 0, BUFFER_SIZE_IN_BYTES)
        }
    }

    @Synchronized
    private fun stopAudioCapture() {
        if (mediaProjection == null)
            return

        recordingCoroutine?.get(); recordingCoroutine = null
        timeMeterCoroutine?.get(); timeMeterCoroutine = null

        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null
        mediaProjection!!.stop()

        recordingExecutor.execute {
            PCMDecoder.encodeToMp3(
                "${Params.instance.pathToSave}/$filename.pcm",
                2,
                128000,
                22000,
                "${Params.instance.pathToSave}/$filename.mp3"
            )

            Params.instance.application.unchecked.contentResolver.insert(
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
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Params.instance.pathToSave)
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.mp3")
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                }
            )
        }

        removeNotification()
        sendBroadcast(Intent(MicRecordService.Broadcast_SET_RECORD_BUTTON_IMAGE))
    }

    @Synchronized
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotification() = startForeground(
       NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, PLAYBACK_RECORDER_CHANNEL_ID)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.cat)
            .setContentTitle(resources.getString(R.string.record_audio))
            .setContentText("${resources.getString(R.string.recording_time)}: $timeMeter ${resources.getString(
                R.string.seconds)}")
            .setAutoCancel(true)
            .setSilent(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    resources.getString(R.string.stop_recording),
                    Intent(this, MicRecordService::class.java).let {
                        it.action = ACTION_STOP
                        PendingIntent.getService(this, 0, it, 0)
                    }
                ).build()
            )
            .build()
    )
}