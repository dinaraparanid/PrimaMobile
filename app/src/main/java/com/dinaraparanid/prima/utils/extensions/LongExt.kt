package com.dinaraparanid.prima.utils.extensions

/** Represents time in minutes as dd:hh:mm */

internal fun Long.toFormattedTimeString(): String {
    var it = this
    val days = it / 1440; it -= (it / 1440) * 1440
    val hours = it / 60; it -= (it / 60) * 60
    return "$days d., $hours h., $it m. ($this m)"
}