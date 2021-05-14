package com.app.musicplayer.database.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.app.musicplayer.core.Album
import com.app.musicplayer.core.Artist

data class ArtistAndAlbum(
    @Embedded val artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "artist_id"
    )
    val album: Album
)
