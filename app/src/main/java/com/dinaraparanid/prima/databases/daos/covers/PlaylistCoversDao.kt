package com.dinaraparanid.prima.databases.daos.covers

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.covers.PlaylistCover
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for playlists' cover */

@Dao
interface PlaylistCoversDao : EntityDao<PlaylistCover> {
    /**
     * Gets playlist with its cover asynchronously
     * @param title playlist's title
     * @return playlist with cover or null if it doesn't exist
     */

    @Query("SELECT * FROM playlists_covers WHERE title = :title")
    suspend fun getPlaylistWithCover(title: String): PlaylistCover?

    /**
     * Removes playlist with its cover asynchronously
     * @param title playlist's title
     */

    @Query("DELETE FROM playlists_covers WHERE title = :title")
    suspend fun removePlaylistWithCover(title: String)

    /**
     * Changes playlist's title
     * @param oldTitle current playlist title
     * @param newTitle new playlist title to set
     */

    @Query("UPDATE playlists_covers SET title = :newTitle WHERE title = :oldTitle")
    suspend fun updatePlaylistTitle(oldTitle: String, newTitle: String)
}