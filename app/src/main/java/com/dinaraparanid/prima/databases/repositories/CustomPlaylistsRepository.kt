package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Repository for user's playlists */

class CustomPlaylistsRepository(context: Context) {
    companion object {
        private const val DATABASE_NAME = "custom_playlists.db"
        private var INSTANCE: CustomPlaylistsRepository? = null
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [CustomPlaylistsRepository] is already initialized
         */

        fun initialize(context: Context) {
            if (INSTANCE != null) throw IllegalStateException("CustomPlaylistsRepository is already initialized")
            INSTANCE = CustomPlaylistsRepository(context)
        }

        /**
         * Gets repository's instance without any synchronization
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        private inline val instance
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("CustomPlaylistsRepository is not initialized")

        /**
         * Gets repository's instance with mutex's protection
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal suspend inline fun getInstanceSynchronized() = mutex.withLock { instance }
    }

    private val database =
        Room
            .databaseBuilder(
                context.applicationContext,
                CustomPlaylistsDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(
                object : Migration(5, 6) {
                    private fun SupportSQLiteDatabase.createTemporaryTrackTable() {
                        execSQL(
                            """
                                CREATE TABLE IF NOT EXISTS CustomTracksTmp(
                                    android_id INTEGER NOT NULL,
                                    id INTEGER NOT NULL PRIMARY KEY,
                                    title TEXT NOT NULL,
                                    artist_name TEXT NOT NULL,
                                    album_title TEXT NOT NULL,
                                    playlist_id INTEGER NOT NULL,
                                    playlist_title TEXT NOT NULL,
                                    path TEXT NOT NULL,
                                    duration INTEGER NOT NULL,
                                    relative_path TEXT,
                                    display_name TEXT,
                                    add_date INTEGER NOT NULL,
                                    track_number_in_album INTEGER NOT NULL,
                                    FOREIGN KEY(playlist_id) REFERENCES CustomPlaylists(id) ON UPDATE CASCADE ON DELETE CASCADE,
                                    FOREIGN KEY(playlist_title) REFERENCES CustomPlaylists(title) ON UPDATE CASCADE ON DELETE CASCADE
                                )
                            """.trimIndent()
                        )

                        execSQL("CREATE INDEX album_title_idx ON CustomPlaylistTrack(album)")
                    }

                    private fun SupportSQLiteDatabase.migrateData() =
                        execSQL("INSERT INTO CustomTracksTmp SELECT * FROM CustomTrack")

                    private fun SupportSQLiteDatabase.removeOldTable() =
                        execSQL("DROP TABLE CustomTracks")

                    private fun SupportSQLiteDatabase.renameTemporaryTrackTable() =
                        execSQL("ALTER TABLE CustomTracksTmp RENAME TO CustomTracks")

                    override fun migrate(database: SupportSQLiteDatabase) =
                        database.run {
                            createTemporaryTrackTable()
                            migrateData()
                            removeOldTable()
                            renameTemporaryTrackTable()
                        }
                }
            )
            .fallbackToDestructiveMigration()
            .build()

    private val tracksDao = database.customPlaylistTracksDao()
    private val playlistsDao = database.customPlaylistsDao()
    private val playlistsAndTracksDao = database.customPlaylistAndTracksDao()

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    internal suspend inline fun getTrackAsync(path: String) =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTrackAsync(path) } }

    /**
     * Gets all playlists with some track asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return list of playlists with given track
     * or empty list if there aren't any playlists with such track
     */

    internal suspend inline fun getPlaylistsByTrackAsync(path: String) =
        coroutineScope { async(Dispatchers.IO) { playlistsDao.getPlaylistsByTrackAsync(path) } }

    /** Updates tracks asynchronously */

    internal suspend inline fun updateTracksAsync(vararg tracks: CustomPlaylistTrack) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.updateAsync(*tracks) } }

    /**
     * Updates track's title, artist and album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     * @param numberInAlbum track's position in album or -1 if no info
     */

    internal suspend inline fun updateTracksAsync(
        path: String,
        title: String,
        artist: String,
        album: String,
        numberInAlbum: Byte
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            tracksDao.updateTrackAsync(path, title, artist, album, numberInAlbum)
        }
    }

    /** Adds tracks asynchronously */

    internal suspend inline fun addTracksAsync(vararg track: CustomPlaylistTrack) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.insertAsync(*track) } }

    /**
     * Removes all tracks with the same path
     * @param path track's path
     */

    internal suspend inline fun removeTrackAsync(path: String) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeTrack(path) } }

    /**
     * Removes track with given path and playlistId asynchronously.
     * Since playlists can contain only unique instances of some track,
     * we can simply say that it removes track from playlist with given id
     * @param path path to track (DATA column from MediaStore)
     * @param playlistId id of playlist
     */

    internal suspend inline fun removeTrackAsync(path: String, playlistId: Long) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeTrackAsync(path, playlistId) } }

    /**
     * Removes all tracks of some playlist asynchronously
     * @param title title of playlist to clear
     */

    internal suspend inline fun removeTracksOfPlaylistAsync(title: String) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeTracksOfPlaylistAsync(title) } }

    /**
     * Gets all playlists asynchronously
     * @return all playlists
     */

    internal suspend inline fun getPlaylistsAsync() =
        coroutineScope { async(Dispatchers.IO) { playlistsDao.getPlaylistsAsync() } }

    /**
     * Gets playlist by it's title asynchronously
     * @param title title of playlist
     * @return playlist if it exists or null
     */

    internal suspend inline fun getPlaylistAsync(title: String) =
        coroutineScope { async(Dispatchers.IO) { playlistsDao.getPlaylistAsync(title) } }

    /**
     * Gets playlist by it's ID asynchronously
     * @param id ID of playlist
     * @return playlist if it exists or null
     */

    internal suspend inline fun getPlaylistAsync(id: Long) =
        coroutineScope { async(Dispatchers.IO) { playlistsDao.getPlaylistAsync(id) } }

    /**
     * Updates playlist's title asynchronously
     * @param oldTitle old playlist's title
     * @param newTitle new title for playlist
     */

    internal suspend inline fun updatePlaylistAsync(oldTitle: String, newTitle: String) =
        coroutineScope {
            launch(Dispatchers.IO) {
                playlistsDao.updatePlaylistAsync(oldTitle, newTitle)
            }
        }

    /**
     * Adds new playlists asynchronously if they weren't exists
     * @param playlists new playlists to add
     */

    internal suspend inline fun addPlaylistsAsync(vararg playlists: CustomPlaylist.Entity) =
        coroutineScope { launch(Dispatchers.IO) { playlistsDao.insertAsync(*playlists) } }

    /**
     *  Deletes playlist by its [title] asynchronously
     *  @param title title of playlist to delete
     */

    internal suspend inline fun removePlaylistAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) {
            playlistsDao.removePlaylistAsync(title)
        }
    }

    /**
     * Gets all playlists with their tracks asynchronously
     * @return all playlists with their tracks
     */

    internal suspend inline fun getPlaylistsWithTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { playlistsAndTracksDao.getPlaylistsWithTracksAsync() } }

    /**
     * Gets all relationships between playlists and tracks asynchronously
     * @return all playlists with their tracks
     */

    internal suspend inline fun getPlaylistsAndTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { playlistsAndTracksDao.getPlaylistsAndTracksAsync() } }

    /**
     * Gets all tracks of playlist by it's title asynchronously
     * @param playlistTitle playlist's title
     * @return tracks of this playlists
     * or empty list if such playlist doesn't exist
     */

    internal suspend inline fun getTracksOfPlaylistAsync(playlistTitle: String) = coroutineScope {
        async(Dispatchers.IO) {
            playlistsAndTracksDao.getTracksOfPlaylistAsync(playlistTitle)
        }
    }

    /**
     * Gets 1-st track of playlist asynchronously
     * @param playlistTitle playlist's title
     * @return 1-st track of this playlists
     * or null if such playlist doesn't exist or empty
     */

    internal suspend inline fun getFirstTrackOfPlaylistAsync(playlistTitle: String) =
        coroutineScope {
            async(Dispatchers.IO) {
                tracksDao.getFirstTrackOfPlaylist(playlistTitle)
            }
        }
}