package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.favourites.*
import com.dinaraparanid.prima.databases.entities.favourites.*

/** Database for user's favourite tracks and artists */

@Database(
    entities = [
        FavouriteTrack::class,
        FavouriteArtist::class,
        FavouritePlaylist.Entity::class,
    ],
    version = 7
)
abstract class FavouriteDatabase : RoomDatabase() {
    /** Creates new [FavouriteTracksDao] */
    abstract fun tracksDao(): FavouriteTracksDao

    /** Creates new [FavouriteArtistsDao] */
    abstract fun artistsDao(): FavouriteArtistsDao

    /** Creates new [FavouritePlaylistsDao] */
    abstract fun playlistsDao(): FavouritePlaylistsDao
}