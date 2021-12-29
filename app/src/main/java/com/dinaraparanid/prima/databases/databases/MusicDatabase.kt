package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinaraparanid.prima.databases.Converters
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.daos.old.*
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
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
internal abstract class MusicDatabase : RoomDatabase() {
    @Deprecated("Now using android storage instead of database")
    abstract fun trackDao(): TrackDao
    @Deprecated("Now using android storage instead of database")
    abstract fun albumDao(): AlbumDao
    @Deprecated("Now using android storage instead of database")
    abstract fun artistDao(): ArtistDao
    @Deprecated("Now using android storage instead of database")
    abstract fun albumAndTrackDao(): AlbumAndTrackDao
    @Deprecated("Now using android storage instead of database")
    abstract fun artistAndTrackDao(): ArtistAndTrackDao
    @Deprecated("Now using android storage instead of database")
    abstract fun artistAndAlbumDao(): ArtistAndAlbumDao
}