package com.dinaraparanid.prima.databases.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for track's album image
 */

@Entity(tableName = "image_tracks")
data class TrackImage(
    @PrimaryKey
    @ColumnInfo(name = "track_path")
    val trackPath: String,
    val image: ByteArray
)