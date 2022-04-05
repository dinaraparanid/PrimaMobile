package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Constructs new playlist from collection of tracks */
internal fun Collection<AbstractTrack>.toPlaylist() = toTypedArray().toPlaylist()

/** Enumerates [AbstractTrack] list with numbers starting from [start] */
internal fun <T : AbstractTrack> Collection<T>.enumerated(start: Int = 0) =
    generateSequence(start) { it + 1 }.asIterable().zip(this)

/** Gets only tracks from enumerated [AbstractTrack] lists */
internal inline val <T> Collection<Pair<Int, T>>.tracks
    get() = map(Pair<Int, T>::second)