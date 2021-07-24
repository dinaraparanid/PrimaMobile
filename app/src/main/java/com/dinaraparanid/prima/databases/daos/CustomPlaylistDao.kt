package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.CustomPlaylist

/**
 * DAO for users playlists
 */

@Dao
interface CustomPlaylistDao {
    /**
     * Gets all playlists asynchronously
     * @return all playlists
     */

    @Query("SELECT * FROM CustomPlaylists")
    suspend fun getPlaylistsAsync(): List<CustomPlaylist.Entity>

    /**
     * Gets playlist by it's title asynchronously
     * @param title title of playlists
     * @return playlist if it exists or null
     */

    @Query("SELECT * FROM CustomPlaylists WHERE title = :title")
    suspend fun getPlaylistAsync(title: String): CustomPlaylist.Entity?

    /**
     * Gets all playlists with some track asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return list of playlists with given track
     * or empty list if there aren't any playlists with such track
     */

    @Query(
        """
        SELECT * FROM CustomPlaylists WHERE id IN (
        SELECT playlist_id FROM CustomTracks WHERE path = :path
        )
    """
    )
    suspend fun getPlaylistsByTrackAsync(path: String): List<CustomPlaylist.Entity>

    /** Updates playlist asynchronously*/

    @Update
    suspend fun updatePlaylistAsync(playlist: CustomPlaylist.Entity)

    /**
     * Adds new playlists if it wasn't exists asynchronously
     * @param playlist new playlist
     */

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlaylistAsync(playlist: CustomPlaylist.Entity)

    /** Deletes playlist asynchronously */

    @Delete
    suspend fun removePlaylistAsync(playlist: CustomPlaylist.Entity)
}