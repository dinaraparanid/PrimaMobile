package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.FavouriteTrack

@Dao
interface FavouriteTrackDao {
    @Query("SELECT * FROM favourite_tracks")
    suspend fun getTracks(): List<FavouriteTrack>

    @Query("SELECT * FROM favourite_tracks WHERE path = :path")
    suspend fun getTrack(path: String): FavouriteTrack?

    @Update
    suspend fun updateTrack(track: FavouriteTrack)

    @Insert
    suspend fun addTrack(track: FavouriteTrack)

    @Delete
    suspend fun removeTrack(track: FavouriteTrack)
}