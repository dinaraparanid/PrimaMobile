package com.dinaraparanid.prima.databases.daos.hidden

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for hidden playlists */

@Dao
interface HiddenPlaylistsDao : EntityDao<HiddenPlaylist.Entity> {

    /**
     * Gets all hidden playlists asynchronously
     * @return all hidden playlists
     */

    @Query("SELECT * FROM hidden_playlists")
    suspend fun getPlaylistsAsync(): List<HiddenPlaylist.Entity>

    /**
     * Gets all hidden playlists with specified [PlaylistType] as int
     * @return all hidden playlists filtered by [type]
     */

    @Query("SELECT * FROM hidden_playlists WHERE type = :type")
    suspend fun getPlaylistsByTypeAsync(type: Int): List<HiddenPlaylist.Entity>

    /**
     * Gets playlists by its title and type
     * @return found playlist or null if it doesn't exist
     */

    @Query("SELECT * FROM hidden_playlists WHERE title = :title AND type = :type")
    suspend fun getPlaylistAsync(title: String, type: Int): HiddenPlaylist.Entity?

    /** Removes playlists by its title and type */
    @Query("DELETE FROM hidden_playlists WHERE title = :title AND type = :type")
    suspend fun removePlaylistAsync(title: String, type: Int)
}