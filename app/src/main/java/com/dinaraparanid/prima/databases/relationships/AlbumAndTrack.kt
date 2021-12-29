package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld

@Deprecated("Now using android storage instead of database")
data class AlbumAndTrack(
    @Embedded val album: AlbumOld,
    @Relation(
        parentColumn = "id",
        entityColumn = "album_id"
    )
    val track: TrackOld
)
