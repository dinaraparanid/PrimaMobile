package com.dinaraparanid.prima.utils.extensions

internal fun <T> T.getBetween(min: T, max: T): T
        where T : Number, T : Comparable<T> {
    if (this < min) return min
    if (this > max) return max
    return this
}