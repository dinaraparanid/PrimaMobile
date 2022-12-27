package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinaraparanid.prima.databases.Converters
import com.dinaraparanid.prima.databases.daos.old.*
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.ArtistOldTrackOldCrossRef

/**
 * Database for all old entities
 * @deprecated Now using android MediaStore instead of database
 */

@Database(
    entities = [
        TrackOld::class,
        AlbumOld::class,
        ArtistOld::class,
        ArtistOldTrackOldCrossRef::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
@Deprecated("Now using android MediaStore instead of database")
abstract class MusicDatabase : RoomDatabase() {
    /** Creates new [TracksDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun tracksDao(): TracksDao

    /** Creates new [AlbumsDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun albumsDao(): AlbumsDao

    /** Creates new [ArtistsDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun artistsDao(): ArtistsDao

    /** Creates new [AlbumsAndTracksDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun albumsAndTracksDao(): AlbumsAndTracksDao

    /** Creates new [ArtistsAndTracksDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun artistsAndTracksDao(): ArtistsAndTracksDao

    /** Creates new [ArtistsAndAlbumsDao] */
    @Deprecated("Now using android MediaStore instead of database")
    abstract fun artistsAndAlbumsDao(): ArtistsAndAlbumsDao
}