package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.custom.CustomPlaylistAndTracksDao
import com.dinaraparanid.prima.databases.daos.custom.CustomPlaylistTracksDao
import com.dinaraparanid.prima.databases.daos.custom.CustomPlaylistsDao
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack

/** Database for user's playlists */

@Database(
    entities = [
        CustomPlaylist.Entity::class,
        CustomPlaylistTrack::class
    ],
    version = 6,
)
abstract class CustomPlaylistsDatabase : RoomDatabase() {
    /** Creates new [CustomPlaylistsDao] */
    abstract fun customPlaylistsDao(): CustomPlaylistsDao

    /** Creates new [CustomPlaylistTracksDao] */
    abstract fun customPlaylistTracksDao(): CustomPlaylistTracksDao

    /** Creates new [CustomPlaylistAndTracksDao] */
    abstract fun customPlaylistAndTracksDao(): CustomPlaylistAndTracksDao
}