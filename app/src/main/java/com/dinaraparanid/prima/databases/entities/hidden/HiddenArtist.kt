package com.dinaraparanid.prima.databases.entities.hidden

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Artist

/** Hidden artist's entity */

@Entity(tableName = "HiddenArtists")
data class HiddenArtist(@PrimaryKey override val name: String) : Artist(name) {
    constructor(artist: Artist) : this(artist.name)
}
