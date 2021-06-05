package com.dinaraparanid.prima.core

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long
)