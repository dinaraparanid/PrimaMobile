package com.dinaraparanid.prima.services

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
import com.dinaraparanid.prima.utils.extensions.rootFile
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import com.google.common.collect.ConcurrentHashMultiset
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private var isAllFileScanRunning = false
    private val filesFounded = AtomicInteger()
    private var files = ConcurrentLinkedQueue<File>()
    private var filesToRemove = ConcurrentHashMultiset.create<String>()
    private val awaitScanningFinishLock = ReentrantLock()
    private val awaitScanningFinishCondition = awaitScanningFinishLock.newCondition()

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
        override fun onReceive(p0: Context?, p1: Intent?) = startScanning(Task.ALL_FILES)
    }

    private val scanSingleFileReceiver = object : BroadcastReceiver() {
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

    override fun onMediaScannerConnected() {
        when (latestTask) {
            Task.ALL_FILES -> launch(Dispatchers.IO) {
                scanAllFilesAsync().orNull()?.join()
            }

            Task.SINGLE_FILE -> scanFile(latestSinglePath!!)
        }
    }

    override fun onScanCompleted(path: String?, uri: Uri?) {
        filesFounded.incrementAndGet()
        path?.let(filesToRemove::remove)

        if (latestTask == Task.ALL_FILES &&
            files.isEmpty() &&
            filesToRemove.isEmpty() &&
            !isAllFileScanRunning
        ) awaitScanningFinishCondition.signal()
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

    private fun scanAllFilesAsync(): Option<Job> {
        if (isAllFileScanRunning)
            return None

        isAllFileScanRunning = true
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
                filesFounded.set(0)

                while (files.isNotEmpty()) files.remove().let { f ->
                    f.listFiles()?.forEach(files::add)
                        ?: f.takeIf(File::isFile)?.run {
                            scanFile(absolutePath)
                            filesToRemove.add(absolutePath)
                        }
                }

                awaitScanningFinishLock.withLock {
                    while (files.isNotEmpty() && filesToRemove.isNotEmpty())
                        awaitScanningFinishCondition.await()

                    connection.disconnect()
                    isAllFileScanRunning = false

                    launch(Dispatchers.Main) {
                        val completionMessage = "${resources.getString(R.string.scan_complete)} $filesFounded"

                        Toast.makeText(
                            applicationContext,
                            completionMessage,
                            Toast.LENGTH_LONG
                        ).show()

                        buildNotification(
                            isLocking = true,
                            notificationType = NotificationType.FINISHED
                        )
                    }
                }
            }
        )
    }

    private fun scanFile(path: String) = connection.scanFile(path, null)

    private fun buildNotificationNoLock(notificationType: NotificationType) =
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(resources.getString(R.string.media_scanner))
                .setContentText(
                    when (notificationType) {
                        NotificationType.SCANNING ->
                            "${resources.getString(R.string.scanning)}..."

                        NotificationType.FINISHED ->
                            "${resources.getString(R.string.scan_complete)} $filesFounded"
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

    private suspend fun buildNotification(
        isLocking: Boolean,
        notificationType: NotificationType
    ) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock(notificationType) }
        else -> buildNotificationNoLock(notificationType)
    }
}