package com.dinaraparanid.prima.services

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.IBinder
import android.media.MediaRecorder
import android.os.Build
import android.provider.MediaStore
import arrow.continuations.generic.update
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** [Service] for audio recording */

class RecordService : Service() {
    private var timeMeter = 0
    private val recordingExecutor = Executors.newFixedThreadPool(2)
    private var timeMeterCoroutine: Future<*>? = null
    private var recordingCoroutine: Future<*>? = null
    private var timeMeterLock = ReentrantLock()
    private var timeMeterCondition = timeMeterLock.newCondition()

    private val iBinder = LocalBinder()
    private var audioRecord: AtomicReference<MediaRecorder> = AtomicReference()
    private var recordingSource = MediaRecorder.AudioSource.DEFAULT
    private var savePath = Params.NO_PATH
    private lateinit var filename: String

    private inline var isRecording
        get() = (application as MainApplication).isRecording
        set(value) { (application as MainApplication).isRecording = value }

    private companion object {
        private inline val curDateAsString
            @JvmStatic
            get() = DateFormat.getInstance().format(Date())
    }

    /**
     * Caller for [RecordService] to start recording.
     * @param application [MainApplication] itself wrapped in a [WeakReference]
     */

    internal class Caller(private val application: WeakReference<MainApplication>) {
        private var recordingSource = MediaRecorder.AudioSource.DEFAULT
        private var filename = curDateAsString

        internal fun setRecordingSource(recordingSource: Int): Caller {
            this.recordingSource = recordingSource
            return this
        }

        internal fun setFileName(filename: String): Caller {
            this.filename = filename
            return this
        }

        private inline val Intent.withExtra
            get() = apply {
                putExtra(MainActivity.RECORDING_SOURCE_ARG, recordingSource)
                putExtra(MainActivity.FILE_NAME_ARG, filename)
            }

        /**
         * Calls of [RecordService] to start recording.
         * If it wasn't previously used, connection will be set and recording will start.
         * If it's already connected, recording will start.
         */

        internal fun call() {
            when {
                !application.unchecked.isRecordingServiceBounded ->
                    application.unchecked.applicationContext.let { context ->
                        val playerIntent = Intent(context, RecordService::class.java).withExtra
                        context.startService(playerIntent)

                        context.bindService(
                            playerIntent,
                            application.unchecked.recordServiceConnection,
                            BIND_AUTO_CREATE
                        )
                    }

                else -> application.unchecked.sendBroadcast(
                    Intent(MainActivity.Broadcast_START_RECORDING).withExtra
                )
            }
        }
    }

    private inner class LocalBinder : Binder() {
        inline val service
            get() = this@RecordService
    }

    private val startRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = startRecording()
    }

    private val stopRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = stopRecording()
    }

    override fun onBind(intent: Intent?): IBinder = iBinder

    override fun onCreate() {
        super.onCreate()
        registerStartRecordingReceiver()
        registerStopRecordingReceiver()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        recordingSource = intent.getIntExtra(
            MainActivity.RECORDING_SOURCE_ARG,
            MediaRecorder.AudioSource.DEFAULT
        )

        filename = intent.getStringExtra(MainActivity.FILE_NAME_ARG)!!.correctFileName
        savePath = "${Params.instance.pathToSave}/$filename.mp3"

        startRecording()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(startRecordingReceiver)
        unregisterReceiver(stopRecordingReceiver)
        stopSelf()
    }

    private fun startRecording() {
        audioRecord = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> MediaRecorder(applicationContext)
            else -> MediaRecorder()
        }.apply {
            setAudioSource(recordingSource)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(savePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
            start()
        }.let(::AtomicReference)

        isRecording = true

        timeMeterCoroutine = recordingExecutor.submit {
            while (isRecording) timeMeterLock.withLock {
                timeMeterCondition.await(1, TimeUnit.SECONDS)
                if (isRecording) timeMeter++
            }
        }
    }

    private fun stopRecording() {
        if (audioRecord.get() != null) {
            isRecording = false
            timeMeterLock.withLock(timeMeterCondition::signal)

            audioRecord.update {
                it.stop()
                it.release()
                it
            }

            audioRecord.set(null)
            recordingCoroutine?.get(); recordingCoroutine = null
            timeMeterCoroutine?.get(); timeMeterCoroutine = null

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
    }

    private fun registerStartRecordingReceiver() = registerReceiver(
        startRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_START_RECORDING)
    )

    private fun registerStopRecordingReceiver() = registerReceiver(
        stopRecordingReceiver,
        IntentFilter(MainActivity.Broadcast_STOP_RECORDING)
    )
}