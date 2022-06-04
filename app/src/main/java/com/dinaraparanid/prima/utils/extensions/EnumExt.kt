package com.dinaraparanid.prima.utils.extensions

/**
 * Gets title of any Enum
 * @example TRACK_COLLECTION -> Track Collection
 */

internal inline val Enum<*>.title
    get() = name
        .split("_")
        .joinToString(separator = " ") { s -> s.lowercase().capitalizeFirst }