package com.dinaraparanid.prima.core

import java.io.Serializable

/**
 * Collection of UNIQUE tracks
 */

abstract class Playlist(open val title: String) : MutableCollection<Track>, Serializable {
    private val tracks: MutableList<Track> = mutableListOf()

    override val size: Int get() = tracks.size
    override fun toString(): String = title
    override fun contains(element: Track): Boolean = element in tracks
    override fun containsAll(elements: Collection<Track>): Boolean = tracks.containsAll(elements)
    override fun isEmpty(): Boolean = tracks.isEmpty()
    override fun clear(): Unit = tracks.clear()
    override fun iterator(): MutableIterator<Track> = tracks.iterator()
    override fun retainAll(elements: Collection<Track>): Boolean = tracks.retainAll(elements)

    constructor(title: String, ts: MutableList<Track>) : this(title) {
        tracks.addAll(ts)
    }

    /**
     * Adds track if it's not in the playlist
     * @return true if track's added;
     * false if such track is already in playlist
     */

    override fun add(element: Track): Boolean = tracks
        .indexOfFirst { it.path == element.path }
        .takeIf { it == -1 }
        ?.run {
            tracks.add(element)
            true
        } ?: false

    /**
     * Adds track from given collection
     * if it's not in the playlist
     * @return true if any of tracks are added;
     * false if all tracks are already in playlist
     */

    override fun addAll(elements: Collection<Track>): Boolean =
        elements.fold(false) { changed, track -> add(track).let { if (!changed) it else true } }

    /**
     * Removes last track
     * which is matching pattern.
     * Also changes current index.
     *
     * @return true if the element has been successfully removed;
     * false if it was not presented in the collection.
     */

    override fun remove(element: Track): Boolean =
        indexOf(element).takeIf { it != -1 }?.let { ind ->
            curIndex = when {
                element.path != currentTrack.path -> if (ind < curIndex) curIndex - 1 else curIndex
                else -> if (curIndex == size) 0 else curIndex
            }

            tracks.remove(element)
        } ?: false

    /**
     * Removes last track
     * which is matching patterns from given collection.
     * Also changes current index.
     *
     * @return true if any of elements have been successfully removed;
     * false if all of tracks were not presented in the collection.
     */

    override fun removeAll(elements: Collection<Track>): Boolean =
        elements.fold(false) { changed, track -> remove(track).let { if (!changed) it else true } }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Playlist) return false
        if (title != other.title) return false
        return true
    }

    override fun hashCode(): Int = title.hashCode()

    private var curIndex: Int = 0

    operator fun get(ind: Int): Track = tracks[ind]

    fun goToPrevTrack() {
        curIndex = if (curIndex == 0) tracks.size - 1 else curIndex - 1
    }

    fun goToNextTrack() {
        curIndex = if (curIndex == tracks.size - 1) 0 else curIndex + 1
    }

    fun toList(): List<Track> = tracks.toList()

    val currentTrack: Track get() = tracks[curIndex]

    inline val prevTrack: Track
        get() {
            goToPrevTrack()
            return currentTrack
        }

    inline val nextTrack: Track
        get() {
            goToNextTrack()
            return currentTrack
        }
}