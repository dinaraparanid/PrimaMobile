package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.images.AlbumImageDao
import com.dinaraparanid.prima.databases.daos.images.PlaylistImageDao
import com.dinaraparanid.prima.databases.daos.images.TrackImageDao
import com.dinaraparanid.prima.databases.entities.images.AlbumImage
import com.dinaraparanid.prima.databases.entities.images.PlaylistImage
import com.dinaraparanid.prima.databases.entities.images.TrackImage

/**
 * Database for images
 */

@Database(
    entities = [TrackImage::class, PlaylistImage::class, AlbumImage::class],
    version = 2
)
abstract class ImagesDatabase : RoomDatabase() {
    abstract fun trackImageDao(): TrackImageDao
    abstract fun playlistImageDao(): PlaylistImageDao
    abstract fun albumImageDao(): AlbumImageDao
}