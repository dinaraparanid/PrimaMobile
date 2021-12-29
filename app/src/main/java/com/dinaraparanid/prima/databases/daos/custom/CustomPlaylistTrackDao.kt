package com.dinaraparanid.prima.databases.daos.custom

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/**
 * DAO for track of playlist
 */

@Dao
interface CustomPlaylistTrackDao : EntityDao<CustomPlaylistTrack> {
    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    @Query("SELECT * FROM CustomTracks WHERE path = :path")
    suspend fun getTrackAsync(path: String): CustomPlaylistTrack?

    /**
     * Removes track with given path and playlistId asynchronously.
     * Since playlists can contain only unique instances of some track,
     * we can simply say that it removes track from playlist with given id
     * @param path path to track (DATA column from MediaStore)
     * @param playlistId id of playlist
     */

    @Query("DELETE FROM CustomTracks WHERE path = :path AND playlist_id = :playlistId")
    suspend fun removeTrackAsync(path: String, playlistId: Long)

    /**
     * Removes all tracks of some playlist asynchronously
     * @param title title of playlist to clear
     */

    @Query("DELETE FROM CustomTracks WHERE playlist_title = :title")
    suspend fun removeTracksOfPlaylistAsync(title: String)

    /**
     * Updates track's title, artist and album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     */

    @Query("UPDATE CustomTracks SET title = :title, artist_name = :artist, playlist_title = :album WHERE path = :path")
    suspend fun updateTrackAsync(path: String, title: String, artist: String, album: String)
}