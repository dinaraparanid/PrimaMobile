package com.dinaraparanid.prima.mvvmp.view.fragments

import android.net.Uri
import carbon.widget.ImageView
import kotlinx.coroutines.Job

/** Interface to change fragment */

interface ChangeImageFragment {
    companion object {
        @Deprecated("Switched to registerForActivityResult")
        const val PICK_IMAGE = 948
    }

    /**
     * Changes [ImageView] source
     * @param image [Uri] of image
     */

    suspend fun setUserImageAsync(image: Uri): Job
}