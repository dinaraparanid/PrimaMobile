package com.dinaraparanid.prima.databases.daos.images

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.images.PlaylistImage
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for playlists' images */

@Dao
interface PlaylistImageDao : EntityDao<PlaylistImage> {
    /**
     * Gets playlist with its image asynchronously
     * @param title playlist's title
     * @return playlist with image or null if it isn't exists
     */

    @Query("SELECT * FROM image_playlists WHERE title = :title")
    suspend fun getPlaylistWithImage(title: String): PlaylistImage?

    /**
     * Removes playlist with its image asynchronously
     * @param title playlist's title
     */

    @Query("DELETE FROM image_playlists WHERE title = :title")
    suspend fun removePlaylistWithImage(title: String)

    /**
     * Changes playlist's title
     * @param oldTitle current playlist title
     * @param newTitle new playlist title to set
     */

    @Query("UPDATE image_playlists SET title = :newTitle WHERE title = :oldTitle")
    suspend fun updatePlaylistTitle(oldTitle: String, newTitle: String)
}