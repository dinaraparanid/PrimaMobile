package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.AlbumImageDao
import com.dinaraparanid.prima.databases.daos.PlaylistImageDao
import com.dinaraparanid.prima.databases.daos.TrackImageDao
import com.dinaraparanid.prima.databases.entities.AlbumImage
import com.dinaraparanid.prima.databases.entities.PlaylistImage
import com.dinaraparanid.prima.databases.entities.TrackImage

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