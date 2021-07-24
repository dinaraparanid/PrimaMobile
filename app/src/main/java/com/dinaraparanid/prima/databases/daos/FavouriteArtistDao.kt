package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.FavouriteArtist

/**
 * DAO for user's favourite artists
 * (singers, compositors and etc.)
 */

@Dao
interface FavouriteArtistDao {
    /**
     * Gets all favourite artists asynchronously
     * @return all favourite artists
     */

    @Query("SELECT * FROM favourite_artists")
    suspend fun getArtistsAsync(): List<FavouriteArtist>

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    @Query("SELECT * FROM favourite_artists WHERE name = :name")
    suspend fun getArtistAsync(name: String): FavouriteArtist?

    /** Updates artist */

    @Update
    suspend fun updateArtistAsync(artist: FavouriteArtist)

    /** Adds new artist asynchronously */

    @Insert
    suspend fun addArtistAsync(artist: FavouriteArtist)

    /** Removes artist asynchronously */

    @Delete
    suspend fun removeArtistAsync(artist: FavouriteArtist)
}