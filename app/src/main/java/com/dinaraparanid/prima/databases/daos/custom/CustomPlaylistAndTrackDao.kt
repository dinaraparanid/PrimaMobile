package com.dinaraparanid.prima.databases.daos.custom

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack

/**
 * DAO for cross playlist and tracks relationship
 */

@Dao
interface CustomPlaylistAndTrackDao {
    /**
     * Gets all playlists with their tracks asynchronously
     * @return all playlists with their tracks
     */

    @Transaction
    @Query("SELECT * FROM CustomPlaylists")
    suspend fun getPlaylistsWithTracksAsync(): List<PlaylistAndTrack>

    /**
     * Gets all tracks of playlist asynchronously
     * @param playlistId id of playlist
     * @return tracks of this playlists
     * or empty list if such playlist doesn't exist
     */

    @Query("SELECT * FROM CustomTracks WHERE playlist_id = :playlistId")
    suspend fun getTracksOfPlaylistAsync(playlistId: Long): List<CustomPlaylistTrack>

    /**
     * Gets 1-st track of playlist asynchronously
     * @param playlistId id of playlist
     * @return 1-st track of this playlists
     * or null if such playlist doesn't exist or empty
     */

    @Query("SELECT * FROM CustomTracks WHERE playlist_id = :playlistId LIMIT 1")
    suspend fun getFirstTrackOfPlaylistAsync(playlistId: Long): CustomPlaylistTrack?
}