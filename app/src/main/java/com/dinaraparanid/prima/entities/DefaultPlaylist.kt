package com.dinaraparanid.prima.entities

import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist

/**
 * Default Playlist that contains tracks
 * without any special properties
 */

class DefaultPlaylist(
    title: String = "No title",
    override val type: PlaylistType = PlaylistType.ALBUM,
    vararg tracks: Track
) : AbstractPlaylist(title.trim(), type, *tracks) {
    override val title = title.trim()

    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 8681912983966612195L
    }
}
