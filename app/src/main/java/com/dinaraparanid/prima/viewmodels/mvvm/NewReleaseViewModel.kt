package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.web.github.Release
import java.lang.ref.WeakReference

/** [ViewModel] for a new version dialog */

class NewReleaseViewModel(
    private val release: Release,
    private val activity: WeakReference<Activity>
) : ViewModel() {
    internal inline val version
        @JvmName("getVersion")
        get() = "${params.application.unchecked.resources.getString(R.string.new_version)}: ${release.name}!"

    internal inline val body
        @JvmName("getBody")
        get() = release.body.replace("[*]".toRegex(), "")

    @JvmName("onUpdateButtonClicked")
    internal fun onUpdateButtonClicked() = activity.unchecked.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://drive.google.com/drive/folders/1eQwkVShbVR2Ev21vWzPZFBzxTQ4r3JNw")
        )
    )
}