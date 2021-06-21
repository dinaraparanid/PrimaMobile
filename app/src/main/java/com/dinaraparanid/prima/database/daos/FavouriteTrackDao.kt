package com.dinaraparanid.prima.database.daos

import androidx.room.*
import com.dinaraparanid.prima.core.FavouriteTrack

@Dao
interface FavouriteTrackDao {
    @Query("SELECT * FROM favourite_tracks")
    fun getTracks(): List<FavouriteTrack>

    @Query("SELECT * FROM favourite_tracks WHERE path = :path")
    fun getTrack(path: String): FavouriteTrack?

    @Update
    fun updateTrack(track: FavouriteTrack)

    @Insert
    fun addTrack(track: FavouriteTrack)

    @Delete
    fun removeTrack(track: FavouriteTrack)
}