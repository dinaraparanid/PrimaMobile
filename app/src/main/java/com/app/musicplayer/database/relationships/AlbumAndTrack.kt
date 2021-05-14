package com.app.musicplayer.database.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.app.musicplayer.core.Album
import com.app.musicplayer.core.Track

data class AlbumAndTrack(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "id",
        entityColumn = "album_id"
    )
    val track: Track
)
