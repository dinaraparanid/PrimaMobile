package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist

/** Default Playlist without any special properties */
class DefaultPlaylist(
    override val title: String = "No title",
    override val type: PlaylistType = PlaylistType.ALBUM,
    vararg tracks: AbstractTrack
) : AbstractPlaylist(title, type, *tracks)
