package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinaraparanid.prima.databases.Converters
import com.dinaraparanid.prima.databases.daos.old.*
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.ArtistTrackCrossRef

/**
 * Database for all old entities
 * @deprecated Now using android MediaStore instead of database
 */

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
@Deprecated("Now using android MediaStore instead of database")
internal abstract class MusicDatabase : RoomDatabase() {
    /** Creates new [TrackDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun trackDao(): TrackDao

    /** Creates new [AlbumDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun albumDao(): AlbumDao

    /** Creates new [ArtistDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun artistDao(): ArtistDao

    /** Creates new [AlbumAndTrackDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun albumAndTrackDao(): AlbumAndTrackDao

    /** Creates new [ArtistAndTrackDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun artistAndTrackDao(): ArtistAndTrackDao

    /** Creates new [ArtistAndAlbumDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun artistAndAlbumDao(): ArtistAndAlbumDao
}