package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.custom.*
import com.dinaraparanid.prima.databases.entities.custom.*

/** Database for user's playlists */

@Database(
    entities = [
        CustomPlaylist.Entity::class,
        CustomPlaylistTrack::class
    ],
    version = 5
)
abstract class CustomPlaylistsDatabase : RoomDatabase() {
    /** Creates new [CustomPlaylistsDao] */
    abstract fun customPlaylistsDao(): CustomPlaylistsDao

    /** Creates new [CustomPlaylistTracksDao] */
    abstract fun customPlaylistTracksDao(): CustomPlaylistTracksDao

    /** Creates new [CustomPlaylistAndTracksDao] */
    abstract fun customPlaylistAndTracksDao(): CustomPlaylistAndTracksDao
}