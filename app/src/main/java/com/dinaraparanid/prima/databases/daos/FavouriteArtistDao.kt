package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.FavouriteArtist

@Dao
interface FavouriteArtistDao {
    @Query("SELECT * FROM favourite_artists")
    suspend fun getArtists(): List<FavouriteArtist>

    @Query("SELECT * FROM favourite_artists WHERE name = (:name)")
    suspend fun getArtist(name: String): FavouriteArtist?

    @Update
    suspend fun updateArtist(artist: FavouriteArtist)

    @Insert
    suspend fun addArtist(artist: FavouriteArtist)

    @Delete
    suspend fun removeArtist(artist: FavouriteArtist)
}