package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import arrow.core.Some
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [ViewModel] that runs conversion and downloads audio from YouTube
 */

@SuppressWarnings("ConstantConditions")
class ConvertFromYouTubeViewModel(
    private val pasteUrlEditText: EditText,
    private val activity: Activity
) : ViewModel() {
    @Volatile
    private var isDownloading = false

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @JvmName("onPasteUrlButtonClicked")
    internal fun onPasteUrlButtonClicked() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            (activity.application as MainApplication)
                .checkAndRequestManageExternalStoragePermission(this::runConversion)
        else -> Some(runConversion())
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

    private fun runConversion() {
        val url = pasteUrlEditText.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(
                activity.applicationContext,
                R.string.url_is_empty,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isStoragePermissionGranted && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            Toast.makeText(
                activity.applicationContext,
                R.string.write_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val addRequest = YoutubeDLRequest(url).apply {
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("-o", "/storage/emulated/0/Music/%(title)s.%(ext)s")
            addOption("--socket-timeout", "1")
            addOption("--retries", "infinite")
        }

        val getInfoRequest = YoutubeDLRequest(url).apply {
            addOption("--get-title")
            addOption("--get-duration")
        }

        Toast.makeText(
            activity.applicationContext,
            R.string.start_conversion,
            Toast.LENGTH_LONG
        ).show()

        isDownloading = true

        executor.execute {
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

                    activity.runOnUiThread {
                        Toast.makeText(
                            activity.applicationContext,
                            when {
                                "Unable to download webpage" in stackTrack -> R.string.no_internet
                                else -> R.string.incorrect_url_link
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@execute
                }
            }

            val out = data.out

            Log.d("DATA", out)

            val (title, timeStr) = out.split('\n').map(String::trim)

            val path = "/storage/emulated/0/Music/${
                title
                    .replace("[|?*<>/']".toRegex(), "_")
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

            params.application.contentResolver.insert(
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

    /** Shows supported for conversion sites */

    @JvmName("onSupportedSitesButtonClicked")
    internal fun onSupportedSitesButtonClicked() = activity.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://ytdl-org.github.io/youtube-dl/supportedsites.html")
        )
    )
}