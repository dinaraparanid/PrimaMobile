package com.app.musicplayer.core

open class Playlist(
    open val title: String = "No title",
    tracks: MutableList<Track>
) {
    override fun toString() = title
}
