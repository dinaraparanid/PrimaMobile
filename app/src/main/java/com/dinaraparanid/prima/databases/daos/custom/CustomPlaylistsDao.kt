package com.dinaraparanid.prima.databases.daos.custom

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for users playlists */

@Dao
interface CustomPlaylistsDao : EntityDao<CustomPlaylist.Entity> {
    /**
     * Gets all playlists asynchronously
     * @return all playlists
     */

    @Query("SELECT * FROM custom_playlists")
    suspend fun getPlaylistsAsync(): List<CustomPlaylist.Entity>

    /**
     * Gets playlist by it's title asynchronously
     * @param title title of playlist
     * @return playlist if it exists or null
     */

    @Query("SELECT * FROM custom_playlists WHERE title = :title")
    suspend fun getPlaylistAsync(title: String): CustomPlaylist.Entity?

    /**
     * Gets playlist by it's ID asynchronously
     * @param id ID of playlist
     * @return playlist if it exists or null
     */

    @Query("SELECT * FROM custom_playlists WHERE id = :id")
    suspend fun getPlaylistAsync(id: Long): CustomPlaylist.Entity?

    /**
     * Gets all playlists with some track asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return list of playlists with given track
     * or empty list if there aren't any playlists with such track
     */

    @Query(
        """
        SELECT * FROM custom_playlists WHERE id IN (
        SELECT playlist_id FROM custom_tracks WHERE path = :path
        )
    """
    )
    suspend fun getPlaylistsByTrackAsync(path: String): List<CustomPlaylist.Entity>

    /**
     * Updates playlist's title asynchronously
     * @param oldTitle old playlist's title
     * @param newTitle new title for playlist
     */

    @Query(
        """
            UPDATE custom_playlists
            SET title = :newTitle
            WHERE id = (SELECT id from custom_playlists WHERE title = :oldTitle)
        """
    )
    suspend fun updatePlaylistAsync(oldTitle: String, newTitle: String)

    /**
     *  Deletes playlist by its [title] asynchronously
     *  @param title title of playlist to delete
     */

    @Query(
        """
            DELETE FROM custom_playlists
            WHERE id = (SELECT id from custom_playlists WHERE title = :title)
        """
    )
    suspend fun removePlaylistAsync(title: String)
}