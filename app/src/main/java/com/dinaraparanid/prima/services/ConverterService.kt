package com.dinaraparanid.prima.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.os.*
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import com.dinaraparanid.prima.viewmodels.mvvm.MP3ConvertViewModel
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.sync.withLock
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/** [Service] for MP3 conversion */

class ConverterService : AbstractService() {
    private companion object {
        private const val CONVERTER_CHANNEL_ID = "mp3_converter_channel"
        private const val NOTIFICATION_ID = 102
        private const val AWAIT_LIMIT = 10000000000L
    }

    private lateinit var lock: Lock
    private lateinit var noTasksCondition: Condition

    private val urls = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadExecutor()
    private val curTrack = AtomicReference<String>()

    private val addTrackToQueueReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(MP3ConvertViewModel.TRACK_URL_ARG)?.let {
                if (it !in urls) {
                    urls.offer(it)
                    lock.withLock(noTasksCondition::signal)

                    curTrack
                        .get()
                        ?.takeIf(String::isNotEmpty)
                        ?.let { runOnWorkerThread { buildNotification(it, isLocking = true) } }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerAddTrackToQueueReceiver()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        intent.getStringExtra(MP3ConvertViewModel.TRACK_URL_ARG).let(urls::offer)

        thread {
            lock = ReentrantLock()
            noTasksCondition = lock.newCondition()

            while (true) {
                var isAwaitLimitExceeded = false

                while (urls.isEmpty())
                    lock.withLock {
                        if (noTasksCondition.awaitNanos(AWAIT_LIMIT) <= 0)
                            isAwaitLimitExceeded = true
                    }

                if (isAwaitLimitExceeded) break
                urls.poll()?.let { executor.submit { startConversion(it) }.get() }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(addTrackToQueueReceiver)
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel() = (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
        .createNotificationChannel(
            NotificationChannel(
                CONVERTER_CHANNEL_ID,
                "MP3 Converter",
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_LOW
                    else -> 0
                }
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )

    override suspend fun handleIncomingActionsNoLock(action: Intent?) = Unit

    private fun registerAddTrackToQueueReceiver() = registerReceiver(
        addTrackToQueueReceiver,
        IntentFilter(MP3ConvertViewModel.Broadcast_ADD_TRACK_TO_QUEUE)
    )

    private fun startConversion(trackUrl: String) {
        val out = try {
            YoutubeDL.getInstance().execute(
                YoutubeDLRequest(trackUrl).apply {
                    addOption("--get-title")
                    addOption("--get-duration")
                }
            ).out
        } catch (e: Exception) {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            e.printStackTrace(printWriter)
            e.printStackTrace()
            val stackTrack = stringWriter.toString()

            runOnUIThread {
                Toast.makeText(
                    applicationContext,
                    when {
                        "Unable to download webpage" in stackTrack -> R.string.no_internet
                        else -> R.string.incorrect_url_link
                    },
                    Toast.LENGTH_LONG
                ).show()
            }

            return
        }

        val (title, timeStr) = out.split('\n').map(String::trim)

        curTrack.set(title)
        runOnWorkerThread { buildNotification(title, isLocking = true) }

        val addRequest = YoutubeDLRequest(trackUrl).apply {
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("-o", "${Params.instance.pathToSave}/%(title)s.%(ext)s")
            addOption("--socket-timeout", "1")
            addOption("--retries", "infinite")
        }

        runOnUIThread {
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

            runOnUIThread {
                Toast.makeText(
                    applicationContext,
                    when {
                        "Unable to download webpage" in stackTrack -> R.string.no_internet
                        else -> R.string.incorrect_url_link
                    },
                    Toast.LENGTH_LONG
                ).show()
            }

            runOnWorkerThread { removeNotification(isLocking = true) }
            return
        }

        val path = "${Params.instance.pathToSave}/${title.correctFileName}.mp3"

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
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Params.instance.pathToSave)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.mp3")
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
            }
        )

        runOnUIThread {
            Toast.makeText(
                applicationContext,
                R.string.conversion_completed,
                Toast.LENGTH_LONG
            ).show()
        }

        curTrack.set(null)
        runOnWorkerThread { removeNotification(isLocking = true) }
    }

    private fun buildNotificationNoLock(track: String) {
        startForeground(
            NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, CONVERTER_CHANNEL_ID)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.octopus)
                .setContentTitle("${resources.getString(R.string.downloading)}: $track")
                .setContentText("${resources.getString(R.string.tracks_in_queue)}: ${urls.size}")
                .setAutoCancel(true)
                .setSilent(true)
                .build()
        )
    }

    private suspend fun buildNotification(track: String, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock(track) }
        else -> buildNotificationNoLock(track)
    }
}