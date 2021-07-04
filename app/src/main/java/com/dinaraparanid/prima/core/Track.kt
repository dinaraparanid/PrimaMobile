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
    fun asCustom(): CustomPlaylistTrack =
        CustomPlaylistTrack(0, title, artist, playlist, path, duration)
}