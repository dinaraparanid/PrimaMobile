package com.dinaraparanid.prima.databases.databases

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.dinaraparanid.prima.databases.daos.covers.*
import com.dinaraparanid.prima.databases.entities.covers.*

/** Database for images */

@Database(
    entities = [TrackCover::class, PlaylistCover::class, AlbumCover::class],
    version = 4,
    autoMigrations = [
        AutoMigration(
            from = 2,
            to = 3,
            spec = CoversDatabase.Migration23::class
        ),
        AutoMigration(
            from = 3,
            to = 4,
            spec = CoversDatabase.Migration34::class
        )
    ]
)
abstract class CoversDatabase : RoomDatabase() {
    @RenameTable.Entries(
        RenameTable(fromTableName = "image_tracks", toTableName = "TracksCovers"),
        RenameTable(fromTableName = "image_albums", toTableName = "AlbumsCovers"),
        RenameTable(fromTableName = "image_playlists", toTableName = "PlaylistsCovers")
    )
    internal class Migration23 : AutoMigrationSpec

    @RenameTable.Entries(
        RenameTable(fromTableName = "TracksCovers", toTableName = "tracks_covers"),
        RenameTable(fromTableName = "PlaylistsCovers", toTableName = "playlists_covers"),
        RenameTable(fromTableName = "AlbumsCovers", toTableName = "albums_covers")
    )
    internal class Migration34 : AutoMigrationSpec

    /** Creates new [TrackCoversDao] */
    abstract fun trackCoversDao(): TrackCoversDao

    /** Creates new [PlaylistCoversDao] */
    abstract fun playlistCoversDao(): PlaylistCoversDao

    /** Creates new [AlbumCoversDao] */
    abstract fun albumCoversDao(): AlbumCoversDao
}