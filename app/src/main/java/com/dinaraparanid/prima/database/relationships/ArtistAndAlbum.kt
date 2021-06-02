package com.dinaraparanid.prima.database.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.core.Album
import com.dinaraparanid.prima.core.Artist

data class ArtistAndAlbum(
    @Embedded val artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "artist_id"
    )
    val album: Album
)
