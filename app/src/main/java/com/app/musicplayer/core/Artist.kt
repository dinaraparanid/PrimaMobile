package com.app.musicplayer.core

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "artist")
data class Artist(
    @PrimaryKey @ColumnInfo(name = "artist_id") val artistId: UUID = UUID.randomUUID(),
    val name: String = "Unknown Artist",
    val info: String = ""
) {
    override fun toString() = name
}