package com.dinaraparanid.prima.databases.entities.old

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "artist")
@Deprecated("Now using android storage instead of database")
data class ArtistOld(
    @PrimaryKey @ColumnInfo(name = "artist_id") val artistId: UUID = UUID.randomUUID(),
    val name: String = "Unknown Artist",
    val info: String = ""
) {
    override fun toString(): String = name
}