package com.dinaraparanid.prima.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.extensions.correctFileName
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.mvvm.MP3ConvertViewModel
import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLRequest
import com.vmadalin.easypermissions.EasyPermissions
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/** [Service] for MP3 conversion */

class ConverterService : AbstractService(), StatisticsUpdatable, CoroutineScope by MainScope() {
    private companion object {
        private const val CONVERTER_CHANNEL_ID = "mp3_converter_channel"
        private const val NOTIFICATION_ID = 102
    }

    private enum class NotificationTarget { CONVERTING, FINISHED }

    private val noTasksCondition = AsyncCondVar()
    private val urls = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadExecutor()
    private val curTrack = AtomicReference<String>()
    override val updateStyle = Statistics::withIncrementedNumberOfConverted
    override val coroutineContext = super.coroutineContext

    private val addTrackToQueueReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(MP3ConvertViewModel.TRACK_URL_ARG)?.let {
                if (it !in urls) {
                    urls.offer(it)
                    runOnUIThread { noTasksCondition.openAsync() }

                    curTrack
                        .get()
                        ?.takeIf(String::isNotEmpty)
                        ?.let {
                            runOnWorkerThread {
                                buildNotificationAsync(
                                    track = it,
                                    target = NotificationTarget.CONVERTING,
                                    isLocking = true
                                )
                            }
                        }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerAddTrackToQueueReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        if (intent == null)
            return START_NOT_STICKY

        intent.getStringExtra(MP3ConvertViewModel.TRACK_URL_ARG).let(urls::offer)

        launch(Dispatchers.IO) {
            while (true) {
                while (urls.isEmpty())
                    noTasksCondition.blockAsync()

                urls.poll()?.let { executor.submit { runBlocking { startConversion(it) } }.get() }
            }
        }

        return START_NOT_STICKY
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

    /** Starts conversion by given URL */
    private suspend fun startConversion(trackUrl: String) {
        val out = try {
            YoutubeDL.execute(
                YoutubeDLRequest(trackUrl).apply {
                    setOption("get-title")
                    setOption("get-duration")
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
        runOnWorkerThread {
            buildNotificationAsync(
                track = title,
                target = NotificationTarget.CONVERTING,
                isLocking = true
            )
        }

        val addRequest = getFromIOThreadAsync {
            YoutubeDLRequest(
                trackUrl,
                "${Params.getInstanceSynchronized().pathToSave}/%(title)s.%(ext)s"
            ).apply {
                setOption("extract-audio")
                setOption("audio-format", "mp3")
                setOption("socket-timeout", "1")
                setOption("retries", "infinite")
            }
        }

        runOnUIThread {
            Toast.makeText(
                applicationContext,
                R.string.start_conversion,
                Toast.LENGTH_LONG
            ).show()
        }

        try {
            YoutubeDL.execute(addRequest.await())
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

            runOnWorkerThread { removeNotificationAsync(isLocking = true) }
            return
        }

        val path = "${Params.getInstanceSynchronized().pathToSave}/${title.correctFileName}.mp3"

        val time = timeStr.split(':').map(String::toInt).run {
            when (size) {
                3 -> get(0) * 3600 + get(1) * 60 + get(2)
                2 -> get(0) * 60 + get(1)
                else -> get(0)
            }.toLong()
        }

        // Insert it into the database

        Params.getInstanceSynchronized().application.unchecked.contentResolver.insert(
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
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Params.getInstanceSynchronized().pathToSave)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.mp3")
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
            }
        )

        updateStatisticsAsync()

        runOnUIThread {
            buildNotificationAsync(target = NotificationTarget.FINISHED, isLocking = true)

            Toast.makeText(
                applicationContext,
                R.string.conversion_completed,
                Toast.LENGTH_LONG
            ).show()
        }

        curTrack.set(null)
    }

    private fun buildNotificationNoLockUnchecked(track: String?, target: NotificationTarget) =
        (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(applicationContext, CONVERTER_CHANNEL_ID)
                .setSmallIcon(R.drawable.octopus)
                .setContentTitle(
                    when (target) {
                        NotificationTarget.CONVERTING ->
                            "${resources.getString(R.string.downloading)}: $track"

                        NotificationTarget.FINISHED ->
                            resources.getString(R.string.mp3_converter)
                    }
                )
                .setContentText(
                    when (target) {
                        NotificationTarget.CONVERTING ->
                            "${resources.getString(R.string.tracks_in_queue)}: ${urls.size}"

                        NotificationTarget.FINISHED ->
                            resources.getString(R.string.conversion_completed)
                    }
                )
                .setShowWhen(false)
                .setAutoCancel(false)
                .setSilent(true)
                .setOngoing(target == NotificationTarget.CONVERTING)
                .build()
        )

    /** Builds notification without any lock */
    private fun buildNotificationNoLock(track: String?, target: NotificationTarget) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                if (EasyPermissions.hasPermissions(
                        applicationContext, Manifest.permission.POST_NOTIFICATIONS
                    )
                ) buildNotificationNoLockUnchecked(track, target)

            else -> buildNotificationNoLockUnchecked(track, target)
        }
    }

    private suspend fun buildNotificationAsync(
        track: String? = null,
        target: NotificationTarget,
        isLocking: Boolean
    ) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock(track, target) }
        else -> buildNotificationNoLock(track, target)
    }
}