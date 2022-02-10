package com.dinaraparanid.prima.databases.daos.custom

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack

/** DAO for cross playlist and tracks relationship */

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
     * @param playlistTitle title of playlist
     * @return tracks of this playlists
     * or empty list if such playlist doesn't exist
     */

    @Query("SELECT * FROM CustomTracks WHERE playlist_title = :playlistTitle")
    suspend fun getTracksOfPlaylistAsync(playlistTitle: String): List<CustomPlaylistTrack>
}