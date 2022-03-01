package com.dinaraparanid.prima.utils.polymorphism

import java.io.Serializable
import java.util.Collections

/** Collection of UNIQUE tracks */

abstract class AbstractPlaylist(
    internal open val title: String,
    internal open val type: PlaylistType
) : MutableList<AbstractTrack>,
    Serializable,
    Comparable<AbstractPlaylist> {
    enum class PlaylistType {
        ALBUM, CUSTOM, GTM
    }

    private var curIndex: Int = 0
    private val tracks: MutableList<AbstractTrack> = Collections.synchronizedList(mutableListOf())

    internal constructor(
        title: String,
        type: PlaylistType,
        vararg ts: AbstractTrack
    ) : this(title.trim(), type) { addAll(ts) }

    final override val size: Int get() = tracks.size
    final override fun toString(): String = "Playlist(title = $title, type = $type, tracks = $tracks)"
    final override fun contains(element: AbstractTrack): Boolean = element in tracks
    final override fun containsAll(elements: Collection<AbstractTrack>): Boolean = tracks.containsAll(elements)
    final override fun isEmpty(): Boolean = tracks.isEmpty()
    final override fun clear(): Unit = tracks.clear()
    final override fun iterator(): MutableIterator<AbstractTrack> = tracks.iterator()
    final override fun retainAll(elements: Collection<AbstractTrack>): Boolean = tracks.retainAll(elements)
    final override fun indexOf(element: AbstractTrack): Int = tracks.indexOf(element)
    final override fun lastIndexOf(element: AbstractTrack): Int = tracks.lastIndexOf(element)
    final override fun listIterator(): MutableListIterator<AbstractTrack> = tracks.listIterator()
    final override fun listIterator(index: Int): MutableListIterator<AbstractTrack> = tracks.listIterator(index)
    final override fun removeAt(index: Int): AbstractTrack = tracks.removeAt(index)
    final override operator fun set(index: Int, element: AbstractTrack): AbstractTrack = tracks.set(index, element)
    final override operator fun get(index: Int): AbstractTrack = tracks[index]

    final override fun subList(fromIndex: Int, toIndex: Int): MutableList<AbstractTrack> =
        tracks.subList(fromIndex, toIndex)

    final override fun addAll(index: Int, elements: Collection<AbstractTrack>): Boolean =
        tracks.addAll(index, elements)

    /**
     * Adds track on [index]ed position
     * if it's not in the playlist
     * or changes it's position
     */

    final override fun add(index: Int, element: AbstractTrack): Unit = tracks
        .indexOfFirst { it.path == element.path }
        .let {
            if (it != -1)
                tracks.removeAt(it)
            tracks.add(index, element)
        }

    /**
     * Adds track if it's not in the playlist
     * or changes it's position
     */

    final override fun add(element: AbstractTrack): Boolean = tracks
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

    final override fun addAll(elements: Collection<AbstractTrack>): Boolean {
        elements.forEach(this::add)
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

    final override fun remove(element: AbstractTrack): Boolean =
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

    final override fun removeAll(elements: Collection<AbstractTrack>): Boolean =
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

    /** Compares playlists by their [title] */

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractPlaylist) return false
        return title == other.title
    }

    /**
     * Compares playlists on equality by their titles
     */

    final override fun compareTo(other: AbstractPlaylist): Int = title.compareTo(other.title)

    final override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + curIndex
        result = 31 * result + tracks.hashCode()
        return result
    }

    /**
     * Gets current track in playlist
     * @return current track in playlist
     */

    internal val currentTrack: AbstractTrack get() = tracks[curIndex]
}