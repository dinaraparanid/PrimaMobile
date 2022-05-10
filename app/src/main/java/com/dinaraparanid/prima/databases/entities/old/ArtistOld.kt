package com.dinaraparanid.prima.databases.entities.old

import androidx.room.ColumnInfo
import androidx.room.Entity as RoomEntity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.databases.Entity as PrimaEntity
import java.util.UUID

/**
 * Entity for an artist
 * @deprecated Now using android MediaStore instead of database
 */

@RoomEntity(tableName = "artist")
@Deprecated("Now using android MediaStore instead of database")
data class ArtistOld(
    @PrimaryKey
    @ColumnInfo(name = "artist_id")
    val artistId: UUID = UUID.randomUUID(),

    val name: String = "Unknown Artist",
    val info: String = ""
) : PrimaEntity {
    override fun toString() = name
}