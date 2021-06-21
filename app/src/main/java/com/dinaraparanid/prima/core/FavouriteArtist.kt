package com.dinaraparanid.prima.core

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_artists")
data class FavouriteArtist(@PrimaryKey override val name: String) : Artist(name) {
    constructor(artist: Artist) : this(artist.name)
}