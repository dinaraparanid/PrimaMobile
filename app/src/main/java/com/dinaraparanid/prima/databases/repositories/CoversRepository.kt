package com.dinaraparanid.prima.databases.repositories

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dinaraparanid.prima.databases.databases.CoversDatabase
import com.dinaraparanid.prima.databases.entities.covers.AlbumCover
import com.dinaraparanid.prima.databases.entities.covers.PlaylistCover
import com.dinaraparanid.prima.databases.entities.covers.TrackCover
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Repository for images */

class CoversRepository private constructor(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "track_images.db"
        private var INSTANCE: CoversRepository? = null
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [CoversRepository] is already initialized
         */

        internal fun initialize(context: Context) {
            if (INSTANCE != null) throw IllegalStateException("CoversRepository is already initialized")
            INSTANCE = CoversRepository(context)
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
                ?: throw UninitializedPropertyAccessException("CoversRepository isn't initialized")

        /**
         * Gets repository's instance with mutex's protection
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        @SuppressLint("SyntheticAccessor")
        internal suspend fun getInstanceSynchronized() = mutex.withLock { instance }
    }

    private val database =
        Room
            .databaseBuilder(
                context.applicationContext,
                CoversDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(
                object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE image_albums (title TEXT NOT NULL, image BLOB NOT NULL, PRIMARY KEY (title))")
                        database.execSQL("CREATE TABLE image_playlists (title TEXT NOT NULL, image BLOB NOT NULL, PRIMARY KEY (title))")
                    }
                },
            )
            .build()

    private val playlistCoversDao = database.playlistCoversDao()
    private val albumCoversDao = database.albumCoversDao()
    private val trackCoversDao = database.trackCoversDao()

    /**
     * Gets track with its cover asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with cover or null if it doesn't exist
     */

    internal suspend fun getTrackWithCoverAsync(path: String) = coroutineScope {
        async(Dispatchers.IO) {
            try {
                trackCoversDao.getTrackWithCover(path)
            } catch (e: Exception) {
                trackCoversDao.removeTrackWithCover(path)
                null
            }
        }
    }

    /** Adds track with its cover asynchronously */
    internal suspend fun addTracksWithCoversAsync(vararg tracks: TrackCover) = coroutineScope {
        launch(Dispatchers.IO) { trackCoversDao.insertAsync(*tracks) }
    }

    /** Removes track with its cover asynchronously */
    internal suspend fun removeTrackWithCoverAsync(path: String) = coroutineScope {
        launch(Dispatchers.IO) { trackCoversDao.removeTrackWithCover(path) }
    }

    /**
     * Gets playlist with its cover asynchronously
     * @param title playlist's title
     * @return playlist with cover or null if it doesn't exist
     */

    internal suspend fun getPlaylistWithCoverAsync(title: String) =
        coroutineScope { async(Dispatchers.IO) { playlistCoversDao.getPlaylistWithCover(title) } }

    /** Removes playlist with its cover asynchronously */
    internal suspend fun removePlaylistWithImageAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) { playlistCoversDao.removePlaylistWithCover(title) }
    }

    /** Adds playlist with its cover asynchronously */
    internal suspend fun addPlaylistsWithImageAsync(vararg playlists: PlaylistCover) =
        coroutineScope { launch(Dispatchers.IO) { playlistCoversDao.insertAsync(*playlists) } }

    /**
     * Changes playlist's title
     * @param oldTitle current playlist title
     * @param newTitle new playlist title to set
     */

    internal suspend fun updatePlaylistTitleAsync(
        oldTitle: String,
        newTitle: String
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            playlistCoversDao.updatePlaylistTitle(oldTitle, newTitle)
        }
    }

    /**
     * Gets album with its cover asynchronously
     * @param title album's title
     * @return album with cover or null if it isn't exists
     */

    internal suspend fun getAlbumWithCoverAsync(title: String) =
        coroutineScope { async(Dispatchers.IO) { albumCoversDao.getAlbumWithCover(title) } }

    /** Removes playlist with its cover asynchronously */
    internal suspend fun removeAlbumWithCoverAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) { albumCoversDao.removeAlbumWithCover(title) }
    }

    /** Adds playlist with its cover asynchronously */
    internal suspend fun addAlbumsWithCoverAsync(vararg albums: AlbumCover) = coroutineScope {
        launch(Dispatchers.IO) { albumCoversDao.insertAsync(*albums) }
    }
}