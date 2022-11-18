package com.dinaraparanid.prima.databases.entities.old

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.room.Entity as RoomEntity
import com.dinaraparanid.prima.utils.polymorphism.databases.Entity as PrimaEntity

/**
 * Entity for an album
 * @deprecated Now using android MediaStore instead of database
 */

@RoomEntity(
    tableName = "album", foreignKeys = [ForeignKey(
        entity = ArtistOld::class,
        parentColumns = arrayOf("artist_id"),
        childColumns = arrayOf("artist_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
@Deprecated("Now using android MediaStore instead of database")
data class AlbumOld(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @ColumnInfo(name = "artist_id", index = true)
    val artistId: UUID? = null,

    val title: String = "Unknown Album",
) : PrimaEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -2795610270160518634L
    }

    override fun toString() = title
}
