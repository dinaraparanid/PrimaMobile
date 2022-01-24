package com.dinaraparanid.prima.databases.daos.custom

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/**
 * DAO for users playlists
 */

@Dao
interface CustomPlaylistDao : EntityDao<CustomPlaylist.Entity> {
    /**
     * Gets all playlists asynchronously
     * @return all playlists
     */

    @Query("SELECT * FROM CustomPlaylists")
    suspend fun getPlaylistsAsync(): List<CustomPlaylist.Entity>

    /**
     * Gets playlist by it's title asynchronously
     * @param title title of playlist
     * @return playlist if it exists or null
     */

    @Query("SELECT * FROM CustomPlaylists WHERE title = :title")
    suspend fun getPlaylistAsync(title: String): CustomPlaylist.Entity?

    /**
     * Gets playlist by it's ID asynchronously
     * @param id ID of playlist
     * @return playlist if it exists or null
     */

    @Query("SELECT * FROM CustomPlaylists WHERE id = :id")
    suspend fun getPlaylistAsync(id: Long): CustomPlaylist.Entity?

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
}