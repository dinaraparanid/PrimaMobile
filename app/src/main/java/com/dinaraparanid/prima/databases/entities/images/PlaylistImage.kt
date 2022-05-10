package com.dinaraparanid.prima.databases.entities.images

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.databases.ImageEntity

/** Entity for playlists' covers */

@Entity(tableName = "image_playlists")
data class PlaylistImage(
    /** Playlist's title */
    @PrimaryKey val title: String,

    /** Cover in bytes */
    override val image: ByteArray
) : ImageEntity {

    /** Compares playlist's covers by its [title] and [image] data */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaylistImage) return false
        if (title != other.title) return false
        if (!image.contentEquals(other.image)) return false
        return true
    }

    /** Hashes [PlaylistImage] by its [title] and [image] data */
    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}