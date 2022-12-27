package com.dinaraparanid.prima.databases.entities.covers

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entity for albums' covers */

@Entity(tableName = "albums_covers")
data class AlbumCover(
    /** Album's title */
    @PrimaryKey val title: String,

    /** Cover in bytes */
    override val image: ByteArray
) : CoverEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -797328093361199688L
    }

    /** Compares album's covers by its [title] */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaylistCover) return false
        if (title != other.title) return false
        return true
    }

    /** Hashes [AlbumCover] by its [title] */
    override fun hashCode() = title.hashCode()
}