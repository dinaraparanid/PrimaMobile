package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack

@Dao
interface CustomPlaylistAndTrackDao {
    @Transaction
    @Query("SELECT * FROM CustomPlaylists")
    suspend fun getPlaylistsWithTracks(): List<PlaylistAndTrack>

    @Query("SELECT * FROM CustomPlaylists WHERE title = (:albumTitle)")
    suspend fun getPlaylistByTrack(albumTitle: String): CustomPlaylist.Entity?

    @Query("SELECT * FROM CustomTracks WHERE playlist_title = (:playlistTitle)")
    suspend fun getTracksOfPlaylist(playlistTitle: String): List<CustomPlaylistTrack>
}