package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefEntity

/**
 * Relationships between [AlbumOld] and [TrackOld]
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
data class AlbumAndTrack(
    @Embedded val album: AlbumOld,
    @Relation(
        parentColumn = "id",
        entityColumn = "album_id"
    )
    val track: TrackOld
) : CrossRefEntity
