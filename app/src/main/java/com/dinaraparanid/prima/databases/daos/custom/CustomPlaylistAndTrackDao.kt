package com.dinaraparanid.prima.databases.daos.custom

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistWithTracks
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefDao

/** [Dao] for cross playlist and tracks relationship */

@Dao
interface CustomPlaylistAndTrackDao : CrossRefDao<PlaylistWithTracks> {

    /**
     * Gets all playlists with their tracks asynchronously
     * @return all playlists with their tracks
     */

    @Transaction
    @Query("SELECT * FROM CustomPlaylists")
    suspend fun getPlaylistsWithTracksAsync(): List<PlaylistWithTracks>

    /**
     * Gets all relationships between playlists and tracks asynchronously
     * @return all playlists with their tracks
     */

    @Transaction
    @Query("SELECT * FROM CustomPlaylists GROUP BY title")
    suspend fun getPlaylistsAndTracksAsync(): List<PlaylistAndTrack>

    /**
     * Gets all tracks of playlist asynchronously
     * @param playlistTitle title of playlist
     * @return tracks of this playlists
     * or empty list if such playlist doesn't exist
     */

    @Query("SELECT * FROM CustomTracks WHERE playlist_title = :playlistTitle")
    suspend fun getTracksOfPlaylistAsync(playlistTitle: String): List<CustomPlaylistTrack>
}