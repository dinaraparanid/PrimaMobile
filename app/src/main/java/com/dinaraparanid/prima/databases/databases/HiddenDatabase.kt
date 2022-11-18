package com.dinaraparanid.prima.databases.databases

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.dinaraparanid.prima.databases.daos.hidden.HiddenArtistsDao
import com.dinaraparanid.prima.databases.daos.hidden.HiddenPlaylistsDao
import com.dinaraparanid.prima.databases.daos.hidden.HiddenTracksDao
import com.dinaraparanid.prima.databases.entities.hidden.HiddenArtist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack

/** Database for hidden entities */

@Database(
    entities = [HiddenTrack::class, HiddenArtist::class, HiddenPlaylist.Entity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(
            from = 2,
            to = 3,
            spec = HiddenDatabase.Migration23::class
        )
    ]
)
abstract class HiddenDatabase : RoomDatabase() {
    @RenameTable.Entries(
        RenameTable(fromTableName = "HiddenArtists", toTableName = "hidden_artists"),
        RenameTable(fromTableName = "HiddenTracks", toTableName = "hidden_tracks"),
        RenameTable(fromTableName = "HiddenPlaylists", toTableName = "hidden_playlists")
    )
    class Migration23 : AutoMigrationSpec

    /** Creates new [HiddenTracksDao] */
    abstract fun hiddenTracksDao(): HiddenTracksDao

    /** Creates new [HiddenArtistsDao] */
    abstract fun hiddenArtistsDao(): HiddenArtistsDao

    /** Creates new [HiddenPlaylistsDao] */
    abstract fun hiddenPlaylistsDao(): HiddenPlaylistsDao
}