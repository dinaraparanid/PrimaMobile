package com.dinaraparanid.prima.databases.databases

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
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
    version = 7,
    autoMigrations = [
        AutoMigration(
            from = 6,
            to = 7,
            spec = CustomPlaylistsDatabase.Migration78::class
        )
    ]
)
abstract class CustomPlaylistsDatabase : RoomDatabase() {
    @RenameTable.Entries(
        RenameTable(fromTableName = "CustomPlaylists", toTableName = "custom_playlists"),
        RenameTable(fromTableName = "CustomTracks", toTableName = "custom_tracks")
    )
    internal class Migration78 : AutoMigrationSpec

    /** Creates new [CustomPlaylistsDao] */
    abstract fun customPlaylistsDao(): CustomPlaylistsDao

    /** Creates new [CustomPlaylistTracksDao] */
    abstract fun customPlaylistTracksDao(): CustomPlaylistTracksDao

    /** Creates new [CustomPlaylistAndTracksDao] */
    abstract fun customPlaylistAndTracksDao(): CustomPlaylistAndTracksDao
}