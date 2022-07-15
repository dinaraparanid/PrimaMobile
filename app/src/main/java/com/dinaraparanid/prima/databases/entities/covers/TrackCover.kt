package com.dinaraparanid.prima.databases.entities.covers

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.databases.ImageEntity

/** Entity for tracks' covers */

@Entity(tableName = "TracksCovers")
data class TrackCover(
    /** Track's path */
    @PrimaryKey
    @ColumnInfo(name = "track_path")
    val trackPath: String,

    /** Cover in bytes */
    override val image: ByteArray
) : ImageEntity {
    /** Compares [TrackCover]s by their [trackPath] and [image] data */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackCover) return false
        if (trackPath != other.trackPath) return false
        if (!image.contentEquals(other.image)) return false
        return true
    }

    /** Hashes [TrackCover] by its [trackPath] and [image] data */
    override fun hashCode(): Int {
        var result = trackPath.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}