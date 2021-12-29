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
    version = 5
)
abstract class FavouriteDatabase : RoomDatabase() {
    abstract fun trackDao(): FavouriteTrackDao
    abstract fun artistDao(): FavouriteArtistDao
    abstract fun playlistDao(): FavouritePlaylistDao
}