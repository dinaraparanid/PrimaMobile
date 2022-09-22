package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.lang.ref.WeakReference

/** MVVM View Model for TrackInfoFragment */

class TrackInfoViewModel(
    private val activity: WeakReference<Activity>,
    private val youtubeUrl: String?
) : ViewModel() {

    /**
     * Sends intent to open
     * YouTube clip of track
     */

    @JvmName("sendYouTubeIntent")
    internal fun sendYouTubeIntent() = youtubeUrl?.let {
        activity.get()?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
    }

    /**
     * Sets color to YouTube url [android.widget.TextView]
     * depending on given url
     * @return App's primary color if url != null else font color
     */

    internal inline val youtubeColor: Int
        @JvmName("getYouTubeColor")
        get() = if (youtubeUrl == null) params.fontColor else params.primaryColor
}