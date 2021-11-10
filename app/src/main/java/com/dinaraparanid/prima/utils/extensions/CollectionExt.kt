package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Constructs new playlist from collection of tracks */
internal fun Collection<AbstractTrack>.toPlaylist() = toTypedArray().toPlaylist()