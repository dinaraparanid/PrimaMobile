package com.dinaraparanid.prima.databases.entities.covers

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.databases.ImageEntity

/** Entity for playlists' covers */

@Entity(tableName = "PlaylistsCovers")
data class PlaylistCover(
    /** Playlist's title */
    @PrimaryKey val title: String,

    /** Cover in bytes */
    override val image: ByteArray
) : ImageEntity {

    /** Compares [PlaylistCover]s by their [title] and [image] data */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaylistCover) return false
        if (title != other.title) return false
        if (!image.contentEquals(other.image)) return false
        return true
    }

    /** Hashes [PlaylistCover] by its [title] and [image] data */
    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}