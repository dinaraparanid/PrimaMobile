package com.dinaraparanid.prima.databases.daos.favourites

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.favourites.FavouritePlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/**
 * [Dao] for user's favourite playlists
 * (both albums and custom playlists)
 */

@Dao
interface FavouritePlaylistsDao : EntityDao<FavouritePlaylist.Entity> {
    /**
     * Gets all favourite playlists asynchronously
     * @return all favourite playlists
     */

    @Query("SELECT * FROM favourite_playlists")
    suspend fun getPlaylistsAsync(): List<FavouritePlaylist.Entity>

    /**
     * Gets playlist by its title and type asynchronously
     * @param title Playlist's title
     * @param type [AbstractPlaylist.PlaylistType] as [Int]
     * @return playlist or null if it doesn't exist
     */

    @Query("SELECT * FROM favourite_playlists WHERE title = :title AND type = :type")
    suspend fun getPlaylistAsync(title: String, type: Int): FavouritePlaylist.Entity?

    /**
     * Updates playlist's title by its title and type
     * @param oldTitle playlist's title before update
     * @param type playlist's type as ordinal
     * @param newTitle new title to set
     */

    @Query("UPDATE favourite_playlists SET title = :newTitle WHERE title = :oldTitle AND type = :type")
    suspend fun updatePlaylistAsync(oldTitle: String, type: Int, newTitle: String)
}