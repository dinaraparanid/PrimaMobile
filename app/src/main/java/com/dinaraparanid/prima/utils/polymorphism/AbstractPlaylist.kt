package com.dinaraparanid.prima.utils.polymorphism

import com.dinaraparanid.prima.core.AbstractTrack
import java.io.Serializable

/**
 * Collection of UNIQUE tracks
 */

abstract class AbstractPlaylist(
    internal open val title: String,
    internal open val type: PlaylistType
) : MutableList<AbstractTrack>,
    Serializable,
    Comparable<AbstractPlaylist> {
    enum class PlaylistType {
        ALBUM, CUSTOM, NEW
    }

    private var curIndex: Int = 0
    private val tracks: MutableList<AbstractTrack> = mutableListOf()

    internal constructor(title: String, type: PlaylistType, vararg ts: AbstractTrack) : this(title, type) {
        tracks.addAll(ts)
    }

    override val size: Int get() = tracks.size
    override fun toString(): String = title
    override fun contains(element: AbstractTrack): Boolean = element in tracks
    override fun containsAll(elements: Collection<AbstractTrack>): Boolean = tracks.containsAll(elements)
    override fun isEmpty(): Boolean = tracks.isEmpty()
    override fun clear(): Unit = tracks.clear()
    override fun iterator(): MutableIterator<AbstractTrack> = tracks.iterator()
    override fun retainAll(elements: Collection<AbstractTrack>): Boolean = tracks.retainAll(elements)
    override fun add(index: Int, element: AbstractTrack): Unit = tracks.add(index, element)
    override fun indexOf(element: AbstractTrack): Int = tracks.indexOf(element)
    override fun lastIndexOf(element: AbstractTrack): Int = tracks.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<AbstractTrack> = tracks.listIterator()
    override fun listIterator(index: Int): MutableListIterator<AbstractTrack> = tracks.listIterator(index)
    override fun removeAt(index: Int): AbstractTrack = tracks.removeAt(index)
    override fun set(index: Int, element: AbstractTrack): AbstractTrack = tracks.set(index, element)
    override operator fun get(index: Int): AbstractTrack = tracks[index]

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<AbstractTrack> =
        tracks.subList(fromIndex, toIndex)

    override fun addAll(index: Int, elements: Collection<AbstractTrack>): Boolean =
        tracks.addAll(index, elements)

    /**
     * Adds track if it's not in the playlist
     * or changes it's position
     */

    override fun add(element: AbstractTrack): Boolean = tracks
        .indexOfFirst { it.path == element.path }
        .let {
            if (it != -1)
                tracks.removeAt(it)

            tracks.add(element)
            true
        }

    /**
     * Adds track from given collection
     * if it's not in the playlist
     * or changes it's position
     */

    override fun addAll(elements: Collection<AbstractTrack>): Boolean {
        elements.forEach(tracks::add)
        return true
    }

    /**
     * Removes last track
     * which is matching pattern.
     * Also changes current index.
     *
     * @return true if the element has been successfully removed;
     * false if it was not presented in the collection.
     */

    override fun remove(element: AbstractTrack): Boolean =
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

    override fun removeAll(elements: Collection<AbstractTrack>): Boolean =
        elements.fold(false) { changed, track -> remove(track).let { if (!changed) it else true } }

    /**
     * Replaces old track in a playlist with new one
     * @param oldTrack track which will be replaced
     * @param newTrack track to override old one
     * @return true if track's changed
     * false if it isn't founded
     */

    internal fun replace(oldTrack: AbstractTrack, newTrack: AbstractTrack): Boolean =
        indexOfFirst { it.path == oldTrack.path }
            .takeIf { it != -1 }
            ?.let {
                this[it] = newTrack
                true
            } ?: false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractPlaylist) return false
        return title == other.title
    }

    override fun hashCode(): Int = title.hashCode()

    /**
     * Compares playlists on equality by their titles
     */

    override fun compareTo(other: AbstractPlaylist): Int = title.compareTo(other.title)

    /**
     * Moves to the previous track if there are some,
     * or goes to the last one in playlist
     */

    internal fun goToPrevTrack() {
        curIndex = if (curIndex == 0) tracks.size - 1 else curIndex - 1
    }

    /**
     * Moves to the next track if there are some,
     * or goes to the first one in playlist
     */

    internal fun goToNextTrack() {
        curIndex = if (curIndex == tracks.size - 1) 0 else curIndex + 1
    }

    /**
     * Gets current track in playlist
     * @return current track in playlist
     */

    internal val currentTrack: AbstractTrack get() = tracks[curIndex]

    /**
     * Gets previous track in playlist
     * and moves to it so current track will be the next track
     * @return previous track in playlist
     */

    internal inline val prevTrack: AbstractTrack
        get() {
            goToPrevTrack()
            return currentTrack
        }

    /**
     * Gets previous track in playlist
     * and moves to it so current track will be the next track
     * @return previous track in playlist
     */

    internal inline val nextTrack: AbstractTrack
        get() {
            goToNextTrack()
            return currentTrack
        }
}