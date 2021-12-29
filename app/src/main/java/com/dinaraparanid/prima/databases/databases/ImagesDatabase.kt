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
    abstract fun trackImageDao(): TrackImageDao
    abstract fun playlistImageDao(): PlaylistImageDao
    abstract fun albumImageDao(): AlbumImageDao
}