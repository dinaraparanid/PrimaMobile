package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.pm.PackageManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.dinaraparanid.prima.R
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [ViewModel] that runs conversion and downloads audio from YouTube
 */

class DownloadFromYouTubeViewModel(
    private val pasteUrlEditText: EditText,
    private val activity: Activity
) : ViewModel() {
    @Volatile
    private var isDownloading = false

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @JvmName("onPasteUrlButtonClicked")
    internal fun onPasteUrlButtonClicked() {
        val url = pasteUrlEditText.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(
                activity.applicationContext,
                R.string.url_is_empty,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isStoragePermissionGranted) {
            Toast.makeText(
                activity.applicationContext,
                R.string.write_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val request = YoutubeDLRequest(url).apply {
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("-o", "/storage/emulated/0/Download/%(title)s.%(ext)s")
        }

        Toast.makeText(
            activity.applicationContext,
            R.string.start_conversion,
            Toast.LENGTH_LONG
        ).show()

        isDownloading = true

        executor.execute {
            YoutubeDL.getInstance().execute(request)

            activity.runOnUiThread {
                Toast.makeText(
                    pasteUrlEditText.context.applicationContext,
                    R.string.conversion_completed,
                    Toast.LENGTH_LONG
                ).show()
            }

            isDownloading = false
        }
    }

    private inline val isStoragePermissionGranted
        get() = when (PackageManager.PERMISSION_GRANTED) {
            activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) -> true

            else -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )

                false
            }
        }
}