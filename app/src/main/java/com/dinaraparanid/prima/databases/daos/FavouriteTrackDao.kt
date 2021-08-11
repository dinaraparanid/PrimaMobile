package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/**
 * DAO for user's favourite tracks
 */

@Dao
interface FavouriteTrackDao : EntityDao<FavouriteTrack> {
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
}