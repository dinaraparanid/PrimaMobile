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
     * Updates playlist's title by its id
     * @param id playlist's id
     * @param title new title
     */

    @Query("UPDATE favourite_playlists SET title = :title, type = :type WHERE id = :id")
    suspend fun updatePlaylistAsync(
        id: Long,
        title: String,
        type: Int = AbstractPlaylist.PlaylistType.CUSTOM.ordinal
    )
}