package com.app.musicplayer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.musicplayer.core.Album
import com.app.musicplayer.core.Artist
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.daos.*
import com.app.musicplayer.database.relationships.ArtistTrackCrossRef

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