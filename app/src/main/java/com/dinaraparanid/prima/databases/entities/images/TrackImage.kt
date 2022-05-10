package com.dinaraparanid.prima.databases.entities.images

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.databases.ImageEntity

/** Entity for tracks' covers */

@Entity(tableName = "image_tracks")
data class TrackImage(
    /** Track's path */
    @PrimaryKey
    @ColumnInfo(name = "track_path")
    val trackPath: String,

    /** Cover in bytes */
    override val image: ByteArray
) : ImageEntity {
    /** Compares track's covers by its [trackPath] and [image] data */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackImage) return false
        if (trackPath != other.trackPath) return false
        if (!image.contentEquals(other.image)) return false
        return true
    }

    /** Hashes [TrackImage] by its [trackPath] and [image] data */
    override fun hashCode(): Int {
        var result = trackPath.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}