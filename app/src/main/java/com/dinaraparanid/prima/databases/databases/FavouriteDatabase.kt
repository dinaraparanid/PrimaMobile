package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.daos.FavouriteArtistDao
import com.dinaraparanid.prima.databases.daos.FavouriteTrackDao

@Database(
    entities = [
        FavouriteTrack::class,
        FavouriteArtist::class
    ],
    version = 3
)
abstract class FavouriteDatabase : RoomDatabase() {
    abstract fun trackDao(): FavouriteTrackDao
    abstract fun artistDao(): FavouriteArtistDao
}