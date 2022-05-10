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
    /** Creates new [FavouriteTrackDao] */
    abstract fun trackDao(): FavouriteTrackDao

    /** Creates new [FavouriteArtistDao] */
    abstract fun artistDao(): FavouriteArtistDao

    /** Creates new [FavouritePlaylistDao] */
    abstract fun playlistDao(): FavouritePlaylistDao
}