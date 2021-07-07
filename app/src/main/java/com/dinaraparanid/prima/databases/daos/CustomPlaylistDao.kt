package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.CustomPlaylist

@Dao
interface CustomPlaylistDao {
    @Query("SELECT * FROM CustomPlaylists ORDER BY title")
    suspend fun getPlaylists(): List<CustomPlaylist.Entity>

    @Query("SELECT * FROM CustomPlaylists WHERE title = :title ORDER BY title")
    suspend fun getPlaylist(title: String): CustomPlaylist.Entity?

    @Query(
        """
        SELECT * FROM CustomPlaylists WHERE id IN (
        SELECT playlist_id FROM CustomTracks WHERE path = :path
        ) ORDER BY title
    """
    )
    suspend fun getPlaylistsByTrack(path: String): List<CustomPlaylist.Entity>

    @Update
    suspend fun updatePlaylist(playlist: CustomPlaylist.Entity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlaylist(playlist: CustomPlaylist.Entity)

    @Delete
    suspend fun removePlaylist(playlist: CustomPlaylist.Entity)
}