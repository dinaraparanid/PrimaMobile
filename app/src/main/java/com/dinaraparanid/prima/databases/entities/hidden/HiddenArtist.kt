package com.dinaraparanid.prima.databases.entities.hidden

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Artist

/** Hidden artist's entity */

@Entity(tableName = "hidden_artists")
data class HiddenArtist(@PrimaryKey override val name: String) : Artist(name) {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 4692165991377942438L
    }

    constructor(artist: Artist) : this(artist.name)
}
