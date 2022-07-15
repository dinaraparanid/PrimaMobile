package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.covers.*
import com.dinaraparanid.prima.databases.entities.covers.*

/** Database for images */

@Database(
    entities = [TrackCover::class, PlaylistCover::class, AlbumCover::class],
    version = 3
)
abstract class CoversDatabase : RoomDatabase() {
    /** Creates new [TrackCoversDao] */
    abstract fun trackCoversDao(): TrackCoversDao

    /** Creates new [PlaylistCoversDao] */
    abstract fun playlistCoversDao(): PlaylistCoversDao

    /** Creates new [AlbumCoversDao] */
    abstract fun albumCoversDao(): AlbumCoversDao
}