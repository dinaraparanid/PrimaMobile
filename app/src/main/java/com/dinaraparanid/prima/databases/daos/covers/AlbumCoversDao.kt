package com.dinaraparanid.prima.databases.daos.covers

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.covers.AlbumCover
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for albums' cover */

@Dao
interface AlbumCoversDao : EntityDao<AlbumCover> {
    /**
     * Gets playlist with its cover asynchronously
     * @param title playlist's title
     * @return playlist with image or null if it isn't exists
     */

    @Query("SELECT * FROM AlbumsCovers WHERE title = :title")
    suspend fun getAlbumWithCover(title: String): AlbumCover?

    /**
     * Removes playlist with its cover asynchronously
     * @param title playlist's title
     */

    @Query("DELETE FROM AlbumsCovers WHERE title = :title")
    suspend fun removeAlbumWithCover(title: String)
}