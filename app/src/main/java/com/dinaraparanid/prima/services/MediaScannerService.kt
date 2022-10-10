package com.dinaraparanid.prima.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.extensions.rootFile
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.vmadalin.easypermissions.EasyPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Scans all files from external storage.
 * If it's found media files that are not in MediaStore,
 * it will add them.
 */

internal class MediaScannerService :
    AbstractService(),
    MediaScannerConnection.MediaScannerConnectionClient {
    private var latestSinglePath: String? = null
    private lateinit var latestTask: Task
    private lateinit var connection: MediaScannerConnection

    @Volatile
    private var isAllFilesScanRunning = false

    @Volatile
    private var isSingleFileScanRunning = false

    private var files = ConcurrentLinkedQueue<File>()
    private val awaitScanningFinishCondition = AsyncCondVar()

    @Deprecated("It's hard to get the real number due to the fact that MediaScanner scans not only audio files")
    private val filesFound = AtomicInteger()

    internal companion object {
        private const val NOTIFICATION_ID = 106
        private const val CHANNEL_ID = "media_scanner_channel"
        internal const val Broadcast_SCAN_ALL_FILES = "com.dinaraparanid.prima.utils.SCAN_ALL_FILES"
        internal const val Broadcast_SCAN_SINGLE_FILE = "com.dinaraparanid.prima.utils.SCAN_SINGLE_FILE"
        internal const val TRACK_TO_SCAN_ARG = "track_to_scan"
    }

    private enum class Task {
        ALL_FILES, SINGLE_FILE
    }

    private enum class NotificationType {
        SCANNING, FINISHED
    }

    private val scanAllFilesReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(p0: Context?, p1: Intent?) = startScanning(Task.ALL_FILES)
    }

    private val scanSingleFileReceiver = object : BroadcastReceiver() {
        @SuppressLint("SyntheticAccessor")
        override fun onReceive(p0: Context?, p1: Intent) =
            startScanning(Task.SINGLE_FILE, p1.getStringExtra(TRACK_TO_SCAN_ARG))
    }

    override fun onCreate() {
        super.onCreate()
        registerScanAllFilesReceiver()
        registerScanSingleFileReceiver()
        connection = MediaScannerConnection(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        when (intent.action) {
            Broadcast_SCAN_ALL_FILES -> startScanning(Task.ALL_FILES)

            else -> startScanning(
                Task.SINGLE_FILE,
                intent.getStringExtra(TRACK_TO_SCAN_ARG)
            )
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanAllFilesReceiver)
        unregisterReceiver(scanSingleFileReceiver)
    }

    /** Starts scanning task */
    override fun onMediaScannerConnected() {
        launch(Dispatchers.IO) {
            when (latestTask) {
                Task.ALL_FILES -> scanAllFilesAsync().orNull()?.join()
                Task.SINGLE_FILE -> latestSinglePath?.let(this@MediaScannerService::scanSingleFile)
            }
        }
    }

    /** Increments files counting */
    override fun onScanCompleted(path: String?, uri: Uri?) {
        if (isSingleFileScanRunning)
            when (uri) {
                null -> {
                    // Android 10+ Error
                    path?.let(this::scanFile)
                    return
                }

                else -> {
                    isSingleFileScanRunning = false
                    connection.disconnect()
                }
            }

        // filesFound.incrementAndGet()

        if (isAllFilesScanRunning) runOnIOThread {
            files.remove()
            runScanningNextFileAsync().join()

            if (files.isEmpty()) {
                isAllFilesScanRunning = false
                connection.disconnect()
                awaitScanningFinishCondition.openAsync()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "media_scanner",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    override suspend fun handleIncomingActionsNoLock(action: Intent?) = Unit

    private fun registerScanAllFilesReceiver() =
        registerReceiver(scanAllFilesReceiver, IntentFilter(Broadcast_SCAN_ALL_FILES))

    private fun registerScanSingleFileReceiver() =
        registerReceiver(scanSingleFileReceiver, IntentFilter(Broadcast_SCAN_SINGLE_FILE))

    private fun startScanning(task: Task, path: String? = null) {
        latestTask = task
        latestSinglePath = path
        connection.connect()
    }

    /**
     * Adds paths to [files] until it's a [File] to scan.
     * If there are no files left, breaks loop
     */

    private fun runScanningNextFileAsync() = runOnIOThread {
        while (true) {
            val isScanStarted = files.peek()
                //.also { Exception("${it?.path}").printStackTrace() }
                ?.takeIf(File::isFile)
                ?.let(File::getAbsolutePath)
                ?.let(this@MediaScannerService::scanFile)
                ?.run { true }
                ?: false

            if (isScanStarted)
                break

            // It's a directory or there are no files left
            files.poll()?.apply {
                listFiles()?.forEach(files::add) // get all tracks from directory
            } ?: break // no files left
        }
    }

    /**
     * Starts scanning for all files
     * if scanner isn't already doing it
     */

    private fun scanAllFilesAsync(): Option<Job> {
        if (isAllFilesScanRunning)
            return None

        isAllFilesScanRunning = true
        files.clear()

        return Some(
            launch(Dispatchers.IO) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        R.string.scan_start,
                        Toast.LENGTH_LONG
                    ).show()

                    buildNotification(
                        isLocking = true,
                        notificationType = NotificationType.SCANNING
                    )
                }

                files.add(rootFile)
                runScanningNextFileAsync()

                // filesFound.set(0)

                while (files.isNotEmpty())
                    awaitScanningFinishCondition.blockAsync()

                launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.scan_complete_no_number),
                        Toast.LENGTH_LONG
                    ).show()

                    buildNotification(
                        isLocking = true,
                        notificationType = NotificationType.FINISHED
                    )
                }
            }
        )
    }


    /**
     * Scans single file.
     * If it wasn't successfully,
     * [onScanCompleted] will restart it
     *
     * @param path path of file to scan
     */

    private fun scanFile(path: String) = connection.scanFile(path, null)

    /**
     * Scans single file if it isn't already
     * scanning another single file.
     * If it wasn't successfully,
     * [onScanCompleted] will restart it
     *
     * @param path path of file to scan
     */

    private fun scanSingleFile(path: String) {
        if (isSingleFileScanRunning)
            return

        isSingleFileScanRunning = true
        connection.scanFile(path, null)
    }

    private fun buildNotificationNoLockUnchecked(notificationType: NotificationType) =
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(resources.getString(R.string.media_scanner))
                .setContentText(
                    when (notificationType) {
                        NotificationType.SCANNING ->
                            "${resources.getString(R.string.scanning)}..."

                        NotificationType.FINISHED ->
                            resources.getString(R.string.scan_complete_no_number)
                    }
                )
                .setSilent(true)
                .setAutoCancel(notificationType == NotificationType.FINISHED)
                .setOngoing(notificationType == NotificationType.SCANNING)
                .setSmallIcon(R.drawable.octopus)
                .setSound(null)
                .setShowWhen(false)
                .build()
        )

    private fun buildNotificationNoLock(notificationType: NotificationType) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                if (EasyPermissions.hasPermissions(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) buildNotificationNoLockUnchecked(notificationType)

            else -> buildNotificationNoLockUnchecked(notificationType)
        }
    }

    private suspend fun buildNotification(
        isLocking: Boolean,
        notificationType: NotificationType
    ) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock(notificationType) }
        else -> buildNotificationNoLock(notificationType)
    }
}