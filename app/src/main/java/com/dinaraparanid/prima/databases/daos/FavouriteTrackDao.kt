package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.FavouriteTrack

/**
 * DAO for user's favourite tracks
 */

@Dao
interface FavouriteTrackDao {
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

    /** Updates track asynchronously */

    @Update
    suspend fun updateTrackAsync(track: FavouriteTrack)

    /** Adds tracks asynchronously */

    @Insert
    suspend fun addTrackAsync(track: FavouriteTrack)

    /** Removes track asynchronously */

    @Delete
    suspend fun removeTrackAsync(track: FavouriteTrack)
}