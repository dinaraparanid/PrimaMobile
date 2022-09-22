package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import arrow.core.Some
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.services.ConverterService
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/** [ViewModel] that runs conversion and downloads audio from YouTube */

@SuppressWarnings("ConstantConditions")
class MP3ConvertViewModel(
    private val pasteUrlEditText: EditText,
    private val activity: WeakReference<Activity>
) : ViewModel() {
    internal companion object {
        internal const val Broadcast_ADD_TRACK_TO_QUEUE = "add_track_to_queue"
        internal const val TRACK_URL_ARG = "track_url"
    }

    @JvmName("onPasteUrlButtonClicked")
    internal fun onPasteUrlButtonClicked() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                (activity.unchecked.application as MainApplication)
                    .checkAndRequestManageExternalStoragePermission(this::runConversion)

            else -> Some(runConversion())
        }
    }

    private inline val isStoragePermissionGranted
        get() = when (PackageManager.PERMISSION_GRANTED) {
            activity.unchecked.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) -> true

            else -> {
                ActivityCompat.requestPermissions(
                    activity.unchecked,
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
                activity.unchecked,
                R.string.url_is_empty,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isStoragePermissionGranted && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            Toast.makeText(
                activity.unchecked,
                R.string.write_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        when {
            !(activity.unchecked.application as MainApplication).isConverterServiceBounded -> {
                val converterIntent = Intent(
                    activity.unchecked.applicationContext,
                    ConverterService::class.java
                ).apply { putExtra(TRACK_URL_ARG, url) }

                activity.unchecked.applicationContext.startService(converterIntent)

                activity.unchecked.applicationContext.bindService(
                    converterIntent,
                    (activity.unchecked.application as MainApplication).converterServiceConnection,
                    AppCompatActivity.BIND_AUTO_CREATE
                )
            }

            else -> activity.unchecked.applicationContext.sendBroadcast(
                Intent(Broadcast_ADD_TRACK_TO_QUEUE).apply { putExtra(TRACK_URL_ARG, url) }
            )
        }
    }

    /** Shows supported for conversion sites */

    @JvmName("onSupportedSitesButtonClicked")
    internal fun onSupportedSitesButtonClicked() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://ytdl-org.github.io/youtube-dl/supportedsites.html")
        )
    )
}