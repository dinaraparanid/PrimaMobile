package com.dinaraparanid.prima.utils.extensions

import android.content.Context
import java.io.File

/**
 * Creates [File] with root location
 */

internal inline val Context.rootFile
    get() = File(
        this.applicationContext
            .getExternalFilesDir(null)!!
            .absolutePath
            .let { if (!it.endsWith("/")) "$it/" else it }
            .split("Android/data/com.dinaraparanid.prima/files/")
            .let { (f, s) -> f + s }
    )