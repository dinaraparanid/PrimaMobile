package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.entities.Track

/** Constructs new playlist from collection of tracks */
internal fun Collection<Track>.toPlaylist() = toTypedArray().toPlaylist()

/** Enumerates [Track] list with numbers starting from [start] */
internal fun <T : Track> Collection<T>.enumerated(start: Int = 0) =
    generateSequence(start) { it + 1 }.asIterable().zip(this)

/** Gets only tracks from enumerated [Track] lists */
internal inline val <T> Collection<Pair<Int, T>>.tracks
    get() = map(Pair<Int, T>::second)