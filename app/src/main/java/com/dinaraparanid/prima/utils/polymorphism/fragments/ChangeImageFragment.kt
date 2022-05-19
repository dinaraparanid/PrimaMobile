package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.net.Uri
import carbon.widget.ImageView
import kotlinx.coroutines.Job

/** Interface to change fragment */

internal interface ChangeImageFragment {
    companion object {
        internal const val PICK_IMAGE = 948
    }

    /**
     * Changes [ImageView] source
     * @param image [Uri] of image
     */

    suspend fun setUserImageAsync(image: Uri): Job
}