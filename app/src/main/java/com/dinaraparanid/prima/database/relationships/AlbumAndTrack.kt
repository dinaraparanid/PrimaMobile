package com.dinaraparanid.prima.database.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.core.Album
import com.dinaraparanid.prima.core.Track

data class AlbumAndTrack(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "id",
        entityColumn = "album_id"
    )
    val track: Track
)
