package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.entities.DefaultPlaylist

/** Constructs new playlist from the given array */
internal fun Array<Track>.toPlaylist() = DefaultPlaylist(tracks = this)

/** Enumerates [Track] array with numbers starting from [start] */
internal fun <T : Track> Array<T>.enumerated(start: Int = 0) = toList().enumerated(start)