package com.dinaraparanid.prima.databases.daos.favourites

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteTrack
import com.dinaraparanid.prima.databases.daos.EntityDao

/** [Dao] for user's favourite tracks */

@Dao
interface FavouriteTracksDao : EntityDao<FavouriteTrack> {
    /**
     * Gets all favourite tracks asynchronously
     * @return all favourite tracks
     */

    @Query("SELECT * FROM favourite_tracks")
    suspend fun getTracksAsync(): List<FavouriteTrack>

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    @Query("SELECT * FROM favourite_tracks WHERE path = :path")
    suspend fun getTrackAsync(path: String): FavouriteTrack?

    /**
     * Updates track's title, artist and album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     * @param numberInAlbum track's position in album or -1 if no info
     */

    @Query("UPDATE favourite_tracks SET title = :title, artist = :artist, album = :album, track_number_in_album = :numberInAlbum WHERE path = :path")
    suspend fun updateTrackAsync(
        path: String,
        title: String,
        artist: String,
        album: String,
        numberInAlbum: Byte
    )

    /**
     * Removes track by its path
     * @param path track's path
     */

    @Query("DELETE FROM favourite_tracks WHERE path = :path")
    suspend fun removeTrack(path: String)
}