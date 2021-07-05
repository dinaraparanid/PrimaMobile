package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.CustomPlaylist

@Dao
interface CustomPlaylistDao {
    @Query("SELECT * FROM CustomPlaylists")
    suspend fun getPlaylists(): List<CustomPlaylist.Entity>

    @Query("SELECT * FROM CustomPlaylists WHERE title = (:title)")
    suspend fun getPlaylist(title: String): CustomPlaylist.Entity?

    @Query("UPDATE CustomPlaylists SET title = (:newTitle) WHERE title = (:oldTitle)")
    suspend fun updatePlaylist(oldTitle: String, newTitle: String)

    @Insert
    suspend fun addPlaylist(playlist: CustomPlaylist.Entity)

    @Delete
    suspend fun removePlaylist(playlist: CustomPlaylist.Entity)
}