package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import java.io.Serializable

open class Track(
    open val title: String,
    open val artist: String,
    open val playlist: String,
    open val path: String,
    open val duration: Long,
) : Serializable, Favourable<FavouriteTrack> {
    override fun asFavourite(): FavouriteTrack = FavouriteTrack(this)
    fun asCustom(playlistTitle: String): CustomPlaylistTrack =
        CustomPlaylistTrack(0, title, artist, playlistTitle, path, duration)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        if (path != other.path) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + playlist.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }
}