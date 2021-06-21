package com.dinaraparanid.prima.database.daos

import androidx.room.*
import com.dinaraparanid.prima.core.FavouriteArtist

@Dao
interface FavouriteArtistDao {
    @Query("SELECT * FROM favourite_artists")
    fun getArtists(): List<FavouriteArtist>

    @Query("SELECT * FROM favourite_artists WHERE name = (:name)")
    fun getArtist(name: String): FavouriteArtist?

    @Update
    fun updateArtist(artist: FavouriteArtist)

    @Insert
    fun addArtist(artist: FavouriteArtist)

    @Delete
    fun removeArtist(artist: FavouriteArtist)
}