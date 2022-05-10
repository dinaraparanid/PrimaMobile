package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dinaraparanid.prima.databases.databases.ImagesDatabase
import com.dinaraparanid.prima.databases.entities.images.AlbumImage
import com.dinaraparanid.prima.databases.entities.images.PlaylistImage
import com.dinaraparanid.prima.databases.entities.images.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Repository for images */

class ImageRepository private constructor(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "track_images.db"

        @JvmStatic
        private var INSTANCE: ImageRepository? = null

        @JvmStatic
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [ImageRepository] is already initialized
         */

        @JvmStatic
        internal fun initialize(context: Context) {
            if (INSTANCE != null) throw IllegalStateException("ImageRepository is already initialized")
            INSTANCE = ImageRepository(context)
        }

        /**
         * Gets repository's instance without any synchronization
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        private inline val instance
            @JvmStatic
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("ImageRepository isn't initialized")

        /**
         * Gets repository's instance with mutex's protection
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        @JvmStatic
        internal suspend fun getInstanceSynchronized() = mutex.withLock { instance }
    }

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            ImagesDatabase::class.java,
            DATABASE_NAME
        )
        .addMigrations(
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE image_albums (title TEXT NOT NULL, image BLOB NOT NULL, PRIMARY KEY (title))")
                    database.execSQL("CREATE TABLE image_playlists (title TEXT NOT NULL, image BLOB NOT NULL, PRIMARY KEY (title))")
                }
            }
        )
        .build()

    private val playlistImageDao = database.playlistImageDao()
    private val albumImageDao = database.albumImageDao()
    private val trackImageDao = database.trackImageDao()

    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    suspend fun getTrackWithImageAsync(path: String) = coroutineScope {
        async(Dispatchers.IO) {
            try {
                trackImageDao.getTrackWithImage(path)
            } catch (e: Exception) {
                trackImageDao.removeTrackWithImage(path)
                null
            }
        }
    }

    /** Adds track with its image asynchronously */
    suspend fun addTrackWithImageAsync(track: TrackImage) = coroutineScope {
        launch(Dispatchers.IO) { trackImageDao.insertAsync(track) }
    }

    /** Removes track with its image asynchronously */
    suspend fun removeTrackWithImageAsync(path: String) = coroutineScope {
        launch(Dispatchers.IO) { trackImageDao.removeTrackWithImage(path) }
    }

    /**
     * Gets playlist with its image asynchronously
     * @param title playlist's title
     * @return playlist with image or null if it isn't exists
     */

    internal suspend fun getPlaylistWithImageAsync(title: String) =
        coroutineScope { async(Dispatchers.IO) { playlistImageDao.getPlaylistWithImage(title) } }

    /** Removes playlist with its image asynchronously */
    internal suspend fun removePlaylistWithImageAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) { playlistImageDao.removePlaylistWithImage(title) }
    }

    /** Adds playlist with its image asynchronously */
    internal suspend fun addPlaylistWithImageAsync(playlist: PlaylistImage) = coroutineScope {
        launch(Dispatchers.IO) { playlistImageDao.insertAsync(playlist) }
    }

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
            playlistImageDao.updatePlaylistTitle(oldTitle, newTitle)
        }
    }

    /**
     * Gets album with its image asynchronously
     * @param title album's title
     * @return album with image or null if it isn't exists
     */

    internal suspend fun getAlbumWithImageAsync(title: String) =
        coroutineScope { async(Dispatchers.IO) { albumImageDao.getAlbumWithImage(title) } }

    /** Removes playlist with its image asynchronously */
    internal suspend fun removeAlbumWithImageAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) { albumImageDao.removeAlbumWithImage(title) }
    }

    /** Adds playlist with its image asynchronously */
    internal suspend fun addAlbumWithImageAsync(album: AlbumImage) = coroutineScope {
        launch(Dispatchers.IO) { albumImageDao.insertAsync(album) }
    }
}