package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.entities.Track

/**
 * Gets tracks for buttons in "Guess The Melody" game
 * @param curInd index of currently correct track that should be guessed
 * @return 3 random tracks with the right one in random order
 */
fun List<Track>.getGTMTracks(curInd: Int = 0) =
    ((this - get(curInd)).shuffled().take(3) + get(curInd)).toPlaylist()


fun <T> MutableList<T>.replace(vararg items: T) {
    clear()
    addAll(items)
}

fun <T> MutableList<T>.replace(items: Collection<T>) {
    clear()
    addAll(items)
}

fun <T> MutableList<T>.replace(items: Iterable<T>) {
    clear()
    addAll(items)
}

fun <T> MutableList<T>.replace(items: Sequence<T>) {
    clear()
    addAll(items)
}