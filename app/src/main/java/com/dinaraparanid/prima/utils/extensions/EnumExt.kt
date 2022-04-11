package com.dinaraparanid.prima.utils.extensions

import java.util.Locale

/**
 * Gets title of any Enum
 * @example TRACK_COLLECTION -> Track Collection
 */

internal inline val Enum<*>.title
    get() = name.split("_").joinToString(separator = " ") { s ->
        s.lowercase().replaceFirstChar {
            when {
                it.isLowerCase() -> it.titlecase(Locale.getDefault())
                else -> it.toString()
            }
        }
    }