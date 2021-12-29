package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.custom.CustomPlaylistDao
import com.dinaraparanid.prima.databases.daos.custom.CustomPlaylistTrackDao
import com.dinaraparanid.prima.databases.daos.custom.CustomPlaylistAndTrackDao
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack

/**
 * Database for user's playlists
 */

@Database(
    entities = [
        CustomPlaylist.Entity::class,
        CustomPlaylistTrack::class
    ],
    version = 3
)
abstract class CustomPlaylistsDatabase : RoomDatabase() {
    abstract fun customPlaylistDao(): CustomPlaylistDao
    abstract fun customPlaylistTrackDao(): CustomPlaylistTrackDao
    abstract fun customPlaylistAndTrackDao(): CustomPlaylistAndTrackDao
}