package com.dinaraparanid.prima.core

/**
 * Default Playlist
 */

class DefaultPlaylist(
    override val title: String = "No title",
    tracks: MutableList<Track> = mutableListOf()
) : Playlist(title, tracks) {
    override fun toString(): String = "DefaultPlaylist(title='$title')"
}
