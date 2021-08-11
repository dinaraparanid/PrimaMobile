package com.dinaraparanid.prima.utils.polymorphism

import android.net.Uri
import carbon.widget.ImageView

/** Interface to change fragment */

internal interface ChangeImageFragment {
    companion object {
        internal const val PICK_IMAGE = 948
    }

    /**
     * Changes [ImageView] source
     * @param image [Uri] of image
     */

    fun setUserImage(image: Uri)
}