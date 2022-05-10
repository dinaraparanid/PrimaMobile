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
    /** Creates new [CustomPlaylistDao] */
    abstract fun customPlaylistDao(): CustomPlaylistDao

    /** Creates new [CustomPlaylistTrackDao] */
    abstract fun customPlaylistTrackDao(): CustomPlaylistTrackDao

    /** Creates new [CustomPlaylistAndTrackDao] */
    abstract fun customPlaylistAndTrackDao(): CustomPlaylistAndTrackDao
}