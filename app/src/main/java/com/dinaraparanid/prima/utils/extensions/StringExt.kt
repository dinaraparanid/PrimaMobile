package com.dinaraparanid.prima.utils.extensions

import java.util.Locale

/** Capitalizes first character of the string */
internal inline val String.capitalizeFirst
    get() = replaceFirstChar {
        when {
            it.isLowerCase() -> it.titlecase(Locale.getDefault())
            else -> it.toString()
        }
    }