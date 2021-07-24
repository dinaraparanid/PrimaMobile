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
    suspend fun getTracks(): List<FavouriteTrack>

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    @Query("SELECT * FROM favourite_tracks WHERE path = :path")
    suspend fun getTrack(path: String): FavouriteTrack?

    /** Updates track asynchronously */

    @Update
    suspend fun updateTrack(track: FavouriteTrack)

    /** Adds tracks asynchronously */

    @Insert
    suspend fun addTrack(track: FavouriteTrack)

    /** Removes track asynchronously */

    @Delete
    suspend fun removeTrack(track: FavouriteTrack)
}