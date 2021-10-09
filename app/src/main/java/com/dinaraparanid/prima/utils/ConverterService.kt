package com.dinaraparanid.prima.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.viewmodels.mvvm.MP3ConvertViewModel
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/* Service for MP3 conversion */

class ConverterService : Service() {
    private companion object {
        private const val CONVERTER_CHANNEL_ID = "mp3_converter_channel"
        private const val NOTIFICATION_ID = 102
        private const val AWAIT_LIMIT = 1000000000L
    }

    private val iBinder: IBinder = LocalBinder()
    private val lock: Lock = ReentrantLock()
    private val noTasksCondition = lock.newCondition()
    private val urls: Queue<String> = ConcurrentLinkedQueue()
    private val executor = Executors.newSingleThreadExecutor()
    private val uiThreadHandler: Handler by lazy { Handler(applicationContext.mainLooper) }
    private var curTrack = ""

    private inner class LocalBinder : Binder() {
        inline val service: ConverterService
            get() = this@ConverterService
    }

    private val addTrackToQueueReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(MP3ConvertViewModel.TRACK_URL_ARG)?.let {
                if (it !in urls) {
                    urls.offer(it)
                    lock.withLock(noTasksCondition::signal)

                    curTrack
                        .takeIf(String::isNotEmpty)
                        ?.let(this@ConverterService::buildNotification)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = iBinder

    override fun onCreate() {
        super.onCreate()
        registerAddTrackToQueueReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        intent?.getStringExtra(MP3ConvertViewModel.TRACK_URL_ARG)?.let(urls::offer)

        thread {
            while (true) {
                lock.withLock {
                    while (urls.isEmpty())
                        noTasksCondition.awaitNanos(AWAIT_LIMIT)

                    urls.poll()?.let { executor.execute { startConversion(it) } }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    CONVERTER_CHANNEL_ID,
                    "MP3 Converter",
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_DEFAULT
                        else -> 0
                    }
                ).apply {
                    this.description = "MP3 Converter channel"
                    setShowBadge(false)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(addTrackToQueueReceiver)
        stopSelf()
    }

    private fun registerAddTrackToQueueReceiver() = registerReceiver(
        addTrackToQueueReceiver,
        IntentFilter(MP3ConvertViewModel.Broadcast_ADD_TRACK_TO_QUEUE)
    )

    private fun startConversion(trackUrl: String) {
        val out = YoutubeDL.getInstance().execute(
            YoutubeDLRequest(trackUrl).apply {
                addOption("--get-title")
                addOption("--get-duration")
            }
        ).out

        val (title, timeStr) = out.split('\n').map(String::trim)

        curTrack = title
        buildNotification(title)

        val addRequest = YoutubeDLRequest(trackUrl).apply {
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("-o", "/storage/emulated/0/Music/%(title)s.%(ext)s")
            addOption("--socket-timeout", "1")
            addOption("--retries", "infinite")
        }

        uiThreadHandler.post {
            Toast.makeText(
                applicationContext,
                R.string.start_conversion,
                Toast.LENGTH_LONG
            ).show()
        }

        try {
            YoutubeDL.getInstance().execute(addRequest)
        } catch (e: Exception) {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            e.printStackTrace(printWriter)
            e.printStackTrace()
            val stackTrack = stringWriter.toString()

            uiThreadHandler.post {
                Toast.makeText(
                    applicationContext,
                    when {
                        "Unable to download webpage" in stackTrack -> R.string.no_internet
                        else -> R.string.incorrect_url_link
                    },
                    Toast.LENGTH_LONG
                ).show()
            }

            removeNotification()
            return
        }

        val path = "/storage/emulated/0/Music/${
            title
                .replace("[|?*<>]".toRegex(), "_")
                .replace(":", " -")
                .replace("\"", "\'")
        }.mp3"

        val time = timeStr.split(':').map(String::toInt).run {
            when (size) {
                3 -> get(0) * 3600 + get(1) * 60 + get(2)
                2 -> get(0) * 60 + get(1)
                else -> get(0)
            }.toLong()
        }

        // Insert it into the database

        Params.instance.application.unchecked.contentResolver.insert(
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Audio.Media.getContentUriForPath(path)!!
            },
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, path)
                put(MediaStore.MediaColumns.TITLE, title)
                put(MediaStore.Audio.Media.DURATION, time * 1000L)
                put(MediaStore.Audio.Media.IS_MUSIC, true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.mp3")
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
            }
        )

        uiThreadHandler.post {
            Toast.makeText(
                applicationContext,
                R.string.conversion_completed,
                Toast.LENGTH_LONG
            ).show()
        }

        curTrack = ""
        removeNotification()
    }

    @Synchronized
    private fun buildNotification(track: String) = startForeground(
        NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, CONVERTER_CHANNEL_ID)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.cat)
            .setContentTitle("${resources.getString(R.string.downloading)}: $track")
            .setContentText("${resources.getString(R.string.tracks_in_queue)}: ${urls.size}")
            .setAutoCancel(true)
            .build()
    )

    @Synchronized
    private fun removeNotification() = stopForeground(true)
}