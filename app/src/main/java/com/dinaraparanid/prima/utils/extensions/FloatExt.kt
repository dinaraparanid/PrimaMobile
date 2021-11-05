package com.dinaraparanid.prima.utils.extensions

/** Returns the [Float] number in borders [0.5..1.5] */

internal inline val Float.playbackParam
    get() = when {
        this in 0.5F..1.5F -> this
        this < 0.5F -> 0.5F
        else -> 1.5F
    }