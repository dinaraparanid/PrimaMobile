package com.dinaraparanid.prima.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.core.FavouriteArtist
import com.dinaraparanid.prima.core.FavouriteTrack
import com.dinaraparanid.prima.database.daos.FavouriteArtistDao
import com.dinaraparanid.prima.database.daos.FavouriteTrackDao

@Database(
    entities = [
        FavouriteTrack::class,
        FavouriteArtist::class
    ],
    version = 1
)
abstract class FavouriteDatabase : RoomDatabase() {
    abstract fun trackDao(): FavouriteTrackDao
    abstract fun artistDao(): FavouriteArtistDao
}