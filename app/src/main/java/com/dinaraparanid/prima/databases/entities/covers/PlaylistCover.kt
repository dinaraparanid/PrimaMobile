package com.dinaraparanid.prima.databases.entities.covers

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entity for playlists' covers */

@Entity(tableName = "playlists_covers")
data class PlaylistCover(
    /** Playlist's title */
    @PrimaryKey val title: String,

    /** Cover in bytes */
    override val image: ByteArray
) : CoverEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 5885489856091607900L
    }

    /** Compares [PlaylistCover]s by their [title] */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaylistCover) return false
        if (title != other.title) return false
        return true
    }

    /** Hashes [PlaylistCover] by its [title] */
    override fun hashCode() = title.hashCode()
}