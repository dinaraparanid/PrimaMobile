package com.dinaraparanid.prima.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.widget.Toast
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.extensions.rootFile
import kotlinx.coroutines.*
import java.io.File

/**
 * Scans all files from external storage.
 * If it's found media files that are not in MediaStore,
 * it will add them.
 */

internal class MediaScanner(private val context: Context) :
    MediaScannerConnection.MediaScannerConnectionClient,
    CoroutineScope by MainScope() {
    private var filesFounded = 0
    private val connection = MediaScannerConnection(context, this)

    override fun onMediaScannerConnected() { scanAllFilesAsync() }
    override fun onScanCompleted(path: String?, uri: Uri?) { filesFounded++ }

    internal fun startScanning() = connection.connect()

    private fun scanAllFilesAsync() = launch(Dispatchers.IO) {
        launch(Dispatchers.Main) {
            Toast.makeText(context, R.string.scan_start, Toast.LENGTH_LONG).show()
        }

        val files = ArrayDeque<File>().apply { add(context.rootFile) }

        while (files.isNotEmpty()) files.removeFirst().let { f ->
            f.listFiles()?.forEach(files::add) ?: f.takeIf(File::isFile)?.run {
                connection.scanFile(absolutePath, null)
            }
        }

        connection.disconnect()

        launch(Dispatchers.Main) {
            Toast.makeText(
                context,
                "${context.resources.getString(R.string.scan_complete)} $filesFounded",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}