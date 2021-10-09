package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.AbstractTrack

/**
 * Constructs new playlist from collection of tracks
 */

fun Collection<AbstractTrack>.toPlaylist(): Playlist = DefaultPlaylist(tracks = toMutableList())