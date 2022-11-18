package com.dinaraparanid.prima.databases.entities.old

import androidx.room.ColumnInfo
import androidx.room.Entity as RoomEntity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.databases.Entity as PrimaEntity
import java.util.UUID

/**
 * Entity for a track
 * @deprecated Now using android MediaStore instead of database
 */

@RoomEntity(
    tableName = "track", foreignKeys = [ForeignKey(
        entity = AlbumOld::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("album_id"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
@Deprecated("Now using android MediaStore instead of database")
data class TrackOld(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: UUID = UUID.randomUUID(),

    val title: String = "Unknown Track",

    @ColumnInfo(name = "album_id", index = true)
    val albumId: UUID? = null,
) : PrimaEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -5930980764623449581L
    }
}

