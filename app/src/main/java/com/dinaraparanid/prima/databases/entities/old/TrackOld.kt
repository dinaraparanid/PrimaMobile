package com.dinaraparanid.prima.databases.entities.old

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "track", foreignKeys = [ForeignKey(
        entity = AlbumOld::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("album_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
@Deprecated("Now using android storage instead of database")
data class TrackOld(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: UUID = UUID.randomUUID(),
    val title: String = "Unknown Track",
    @ColumnInfo(name = "album_id", index = true) val albumId: UUID? = null,
)

