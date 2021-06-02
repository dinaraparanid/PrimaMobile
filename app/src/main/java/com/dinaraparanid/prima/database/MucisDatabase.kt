package com.dinaraparanid.prima.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinaraparanid.prima.core.Album
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.daos.*
import com.dinaraparanid.prima.database.relationships.ArtistTrackCrossRef

@Database(
    entities = [
        Track::class,
        Album::class,
        Artist::class,
        ArtistTrackCrossRef::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumAndTrackDao(): AlbumAndTrackDao
    abstract fun artistAndTrackDao(): ArtistAndTrackDao
    abstract fun artistAndAlbumDao(): ArtistAndAlbumDao
}