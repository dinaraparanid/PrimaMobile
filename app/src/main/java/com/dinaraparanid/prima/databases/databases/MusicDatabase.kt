package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinaraparanid.prima.databases.Converters
import com.dinaraparanid.prima.databases.entities.AlbumOld
import com.dinaraparanid.prima.databases.entities.ArtistOld
import com.dinaraparanid.prima.databases.entities.TrackOld
import com.dinaraparanid.prima.databases.daos.*
import com.dinaraparanid.prima.databases.relationships.ArtistTrackCrossRef

@Database(
    entities = [
        TrackOld::class,
        AlbumOld::class,
        ArtistOld::class,
        ArtistTrackCrossRef::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
@Deprecated("Now using android storage instead of database")
abstract class MusicDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumAndTrackDao(): AlbumAndTrackDao
    abstract fun artistAndTrackDao(): ArtistAndTrackDao
    abstract fun artistAndAlbumDao(): ArtistAndAlbumDao
}