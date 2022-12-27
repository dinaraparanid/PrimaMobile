package com.dinaraparanid.prima.databases.entities.covers

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entity for tracks' covers */

@Entity(tableName = "tracks_covers")
data class TrackCover(
    /** Track's path */
    @PrimaryKey
    @ColumnInfo(name = "track_path")
    val trackPath: String,

    /** Cover in bytes */
    override val image: ByteArray
) : CoverEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -7623534039089060762L
    }

    /** Compares [TrackCover]s by their [trackPath] */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackCover) return false
        if (trackPath != other.trackPath) return false
        return true
    }

    /** Hashes [TrackCover] by its [trackPath] */
    override fun hashCode() = trackPath.hashCode()
}