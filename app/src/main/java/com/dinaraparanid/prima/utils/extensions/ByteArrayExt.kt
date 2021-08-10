package com.dinaraparanid.prima.utils.extensions

import android.graphics.BitmapFactory
import com.dinaraparanid.prima.utils.ViewSetter

/**
 * Converts [ByteArray] to [android.graphics.Bitmap]
 */

internal fun ByteArray.toBitmap() = BitmapFactory
    .decodeByteArray(this, 0, size)
    .let { ViewSetter.getPictureInScale(it, it.width, it.height) }