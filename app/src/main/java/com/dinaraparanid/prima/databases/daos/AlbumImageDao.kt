package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.AlbumImage
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/** [Dao] for albums' images */

@Dao
interface AlbumImageDao : EntityDao<AlbumImage> {
    /**
     * Gets playlist with its image asynchronously
     * @param title playlist's title
     * @return playlist with image or null if it isn't exists
     */

    @Query("SELECT * FROM image_albums WHERE title = :title")
    suspend fun getAlbumWithImage(title: String): AlbumImage?

    /**
     * Removes playlist with its image asynchronously
     * @param title playlist's title
     */

    @Query("DELETE FROM image_albums WHERE title = :title")
    suspend fun removeAlbumWithImage(title: String)
}