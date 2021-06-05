package com.dinaraparanid.prima.database.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.core.AlbumOld
import com.dinaraparanid.prima.core.TrackOld

@Deprecated("Now using android storage instead of database")
data class AlbumAndTrack(
    @Embedded val album: AlbumOld,
    @Relation(
        parentColumn = "id",
        entityColumn = "album_id"
    )
    val track: TrackOld
)
