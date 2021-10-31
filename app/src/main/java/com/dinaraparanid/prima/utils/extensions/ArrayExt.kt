package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.core.DefaultPlaylist

/** Constructs new playlist from the given array */
internal fun Array<AbstractTrack>.toPlaylist() = DefaultPlaylist(tracks = this)