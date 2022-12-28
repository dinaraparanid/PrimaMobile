package com.dinaraparanid.prima.databases.entities.favourites

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.entities.Artist

/** User's favourite artist's entity */

@Entity(tableName = "favourite_artists")
data class FavouriteArtist(@PrimaryKey override val name: String) : Artist(name) {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID: Long = -4831853630448966364L
    }

    constructor(artist: Artist) : this(artist.name)
}