package com.dinaraparanid.prima.utils

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.ConvertFromYouTubeFragment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class ConverterService : Service() {
    private val iBinder: IBinder = LocalBinder()
    private val tasks: Queue<String> = ConcurrentLinkedQueue()
    private val executor = Executors.newFixedThreadPool(3)

    private inner class LocalBinder : Binder() {
        inline val service: ConverterService
            get() = this@ConverterService
    }

    private val addTrackToQueueReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(ConvertFromYouTubeFragment.TRACK_URL_ARG)?.let(tasks::offer)
        }
    }

    override fun onBind(intent: Intent?): IBinder = iBinder

    override fun onCreate() {
        super.onCreate()
        registerAddTrackToQueueReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(ConvertFromYouTubeFragment.TRACK_URL_ARG)?.let { tasks.add(it) }

        val lock = ReentrantLock()
        val noTasksCondition = lock.newCondition()

        thread {
            lock.withLock {
                while (tasks.isEmpty())
                    noTasksCondition.await()

                executor.execute { runBlocking { startConversion(tasks.poll()!!) } }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun registerAddTrackToQueueReceiver() = registerReceiver(
        addTrackToQueueReceiver,
        IntentFilter(ConvertFromYouTubeFragment.Broadcast_ADD_TRACK_TO_QUEUE)
    )

    private suspend fun startConversion(trackUrl: String) = coroutineScope {
        val getInfoRequest = YoutubeDLRequest(trackUrl).apply {
            addOption("--get-title")
            addOption("--get-duration")
        }

        val addRequest = YoutubeDLRequest(trackUrl).apply {
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("-o", "/storage/emulated/0/Music/%(title)s.%(ext)s")
            addOption("--socket-timeout", "1")
            addOption("--retries", "infinite")
        }

        launch(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                R.string.start_conversion,
                Toast.LENGTH_LONG
            ).show()
        }

        val data = YoutubeDL.getInstance().run {
            try {
                execute(addRequest)
                execute(getInfoRequest)
            } catch (e: Exception) {
                val stringWriter = StringWriter()
                val printWriter = PrintWriter(stringWriter)
                e.printStackTrace(printWriter)
                e.printStackTrace()
                val stackTrack = stringWriter.toString()

                launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        when {
                            "Unable to download webpage" in stackTrack -> R.string.no_internet
                            else -> R.string.incorrect_url_link
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }

                return@coroutineScope
            }
        }

        val out = data.out

        Log.d("DATA", out)

        val (title, timeStr) = out.split('\n').map(String::trim)

        val path = "/storage/emulated/0/Music/${
            title
                .replace("[|?*<>]".toRegex(), "_")
                .replace(":", " -")
        }.mp3"

        val time = timeStr.split(':').map(String::toInt).run {
            when (size) {
                3 -> get(0) * 3600 + get(1) * 60 + get(2)
                2 -> get(0) * 60 + get(1)
                else -> get(0)
            }.toLong()
        }

        // Insert it into the database

        Params.instance.application.contentResolver.insert(
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

        launch(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                R.string.conversion_completed,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}