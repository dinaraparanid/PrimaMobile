package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.core.DefaultPlaylist

/** Constructs new playlist from the given array */
internal fun Array<AbstractTrack>.toPlaylist() = DefaultPlaylist(tracks = this)

/** Enumerates [AbstractTrack] array with numbers starting from [start] */
internal fun <T : AbstractTrack> Array<T>.enumerated(start: Int = 0) = toList().enumerated(start)