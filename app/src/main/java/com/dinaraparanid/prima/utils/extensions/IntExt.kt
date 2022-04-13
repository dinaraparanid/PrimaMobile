package com.dinaraparanid.prima.utils.extensions

import android.content.Context
import androidx.annotation.Px

/** Converts pixels to dp */

@Px
internal fun Int.toDp(context: Context) =
    (this * context.resources.displayMetrics.density + 0.5F).toInt()