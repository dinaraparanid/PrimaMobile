package com.dinaraparanid.prima.databases.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.ImageEntity

/**
 * Entity for tracks' album images
 */

@Deprecated("Now changing track's cover's tag with JAudioTag")
@Entity(tableName = "image_tracks")
data class TrackImage(
    @PrimaryKey
    @ColumnInfo(name = "track_path")
    val trackPath: String,
    override val image: ByteArray
) : ImageEntity {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackImage) return false

        if (trackPath != other.trackPath) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackPath.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}