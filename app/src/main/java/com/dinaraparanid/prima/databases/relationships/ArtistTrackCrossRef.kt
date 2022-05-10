package com.dinaraparanid.prima.databases.relationships

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.UUID

/**
 * Junction between artist and track
 * @deprecated Now using android MediaStore instead of database
 */

@Entity(tableName = "artist_track", primaryKeys = ["artist_id", "track_id"])
@Deprecated("Now using android MediaStore instead of database")
data class ArtistTrackCrossRef(
    @ColumnInfo(name = "artist_id", index = true) val artistId: UUID,
    @ColumnInfo(name = "track_id", index = true) val trackId: UUID
)
