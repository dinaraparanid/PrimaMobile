package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.ArtistOld

@Deprecated("Now using android storage instead of database")
data class ArtistAndAlbum(
    @Embedded val artist: ArtistOld,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "artist_id"
    )
    val album: AlbumOld
)
