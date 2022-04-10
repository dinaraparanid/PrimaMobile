package com.dinaraparanid.prima.utils.extensions

import android.content.Context

/** Converts pixels to dp */

internal fun Int.toDp(context: Context) =
    (this * context.resources.displayMetrics.density + 0.5F).toInt()