package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.images.*
import com.dinaraparanid.prima.databases.entities.images.*

/** Database for images */

@Database(
    entities = [TrackImage::class, PlaylistImage::class, AlbumImage::class],
    version = 2
)
abstract class ImagesDatabase : RoomDatabase() {
    /** Creates new [TrackImageDao] */
    abstract fun trackImageDao(): TrackImageDao

    /** Creates new [PlaylistImageDao] */
    abstract fun playlistImageDao(): PlaylistImageDao

    /** Creates new [AlbumImageDao] */
    abstract fun albumImageDao(): AlbumImageDao
}