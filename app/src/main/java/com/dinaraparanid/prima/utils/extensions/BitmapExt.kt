package com.dinaraparanid.prima.utils.extensions

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/**
 * Converts [Bitmap] to [ByteArray]
 */

internal fun Bitmap.toByteArray() = ByteArrayOutputStream()
    .also { compress(Bitmap.CompressFormat.JPEG, 100, it) }
    .toByteArray()