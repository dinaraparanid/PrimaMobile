package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.TrackImageDao
import com.dinaraparanid.prima.databases.entities.TrackImage

/**
 * Database for track's images
 */

@Database(
    entities = [TrackImage::class],
    version = 1
)
abstract class TrackImagesDatabase : RoomDatabase() {
    abstract fun trackImageDao(): TrackImageDao
}