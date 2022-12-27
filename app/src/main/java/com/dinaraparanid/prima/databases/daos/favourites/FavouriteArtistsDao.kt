package com.dinaraparanid.prima.databases.daos.favourites

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteArtist
import com.dinaraparanid.prima.databases.daos.EntityDao

/**
 * [Dao] for user's favourite artists
 * (singers, compositors and etc.)
 */

@Dao
interface FavouriteArtistsDao : EntityDao<FavouriteArtist> {
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
}