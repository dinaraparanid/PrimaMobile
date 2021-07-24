package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.utils.polymorphism.Playlist

/** Default Playlist without any special properties */
class DefaultPlaylist(
    override val title: String = "No title",
    tracks: MutableList<Track> = mutableListOf()
) : Playlist(title, tracks) {
    override fun toString(): String = "DefaultPlaylist(title='$title')"
}
