package com.dinaraparanid.prima.core

import java.io.Serializable

class Playlist(
    val title: String = "No title",
    private val tracks: MutableList<Track> = mutableListOf(),
    @Deprecated(
        """
        Not real size, 
        only for compatibility with MutableCollection<T>. 
        See realSize instead
    """
    )
    override val size: Int = tracks.size,
) : MutableCollection<Track>, Serializable {
    class List(val playlists: Array<Playlist>) : Serializable

    override fun toString(): String = title
    override fun contains(element: Track): Boolean = element in tracks
    override fun containsAll(elements: Collection<Track>): Boolean = tracks.containsAll(elements)
    override fun isEmpty(): Boolean = tracks.isEmpty()
    override fun add(element: Track): Boolean = tracks.add(element)
    override fun addAll(elements: Collection<Track>): Boolean = tracks.addAll(elements)
    override fun clear(): Unit = tracks.clear()
    override fun iterator(): MutableIterator<Track> = tracks.iterator()
    override fun remove(element: Track): Boolean = tracks.remove(element)
    override fun removeAll(elements: Collection<Track>): Boolean = tracks.removeAll(elements)
    override fun retainAll(elements: Collection<Track>): Boolean = tracks.retainAll(elements)

    val realSize: Int
        get() = tracks.size

    private var curIndex: Int = 0

    operator fun get(ind: Int): Track = tracks[ind]

    fun goToPrevTrack() {
        curIndex = if (curIndex == 0) tracks.size - 1 else curIndex - 1
    }

    fun goToNextTrack() {
        curIndex = if (curIndex == tracks.size - 1) 0 else curIndex + 1
    }

    fun toList(): kotlin.collections.List<Track> = tracks.toList()

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
