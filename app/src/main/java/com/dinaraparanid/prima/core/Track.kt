package com.dinaraparanid.prima.core

import java.io.Serializable

open class Track(
    open val title: String,
    open val artist: String,
    open val album: String,
    open val path: String,
    open val duration: Long,
    open val albumId: Long
) : Serializable, Favourable<FavouriteTrack> {
    override fun asFavourite(): FavouriteTrack = FavouriteTrack(this)
}