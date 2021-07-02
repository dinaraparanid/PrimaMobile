package com.dinaraparanid.prima.databases.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Artist

@Entity(tableName = "favourite_artists")
data class FavouriteArtist(@PrimaryKey override val name: String) : Artist(name) {
    constructor(artist: Artist) : this(artist.name)
}