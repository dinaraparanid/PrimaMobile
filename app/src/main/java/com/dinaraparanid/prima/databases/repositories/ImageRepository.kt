package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dinaraparanid.prima.databases.databases.ImagesDatabase
import com.dinaraparanid.prima.databases.entities.AlbumImage
import com.dinaraparanid.prima.databases.entities.PlaylistImage
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.*

/** Repository for images */

class ImageRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "track_images.db"
        private var INSTANCE: ImageRepository? = null

        /**
         * Initialises repository only once
         */

        internal fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = ImageRepository(context)
        }

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal val instance: ImageRepository
            @Synchronized
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("ImageRepository is not initialized")
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

    private val trackImageDao = database.trackImageDao()
    private val playlistImageDao = database.playlistImageDao()
    private val albumImageDao = database.albumImageDao()

    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    suspend fun getTrackWithImageAsync(path: String): Deferred<TrackImage?> = coroutineScope {
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

    suspend fun addTrackWithImageAsync(track: TrackImage): Job = coroutineScope {
        launch(Dispatchers.IO) { trackImageDao.insertAsync(track) }
    }

    /** Removes track with its image asynchronously */

    suspend fun removeTrackWithImageAsync(path: String): Job = coroutineScope {
        launch(Dispatchers.IO) { trackImageDao.removeTrackWithImage(path) }
    }

    /**
     * Gets playlist with its image asynchronously
     * @param title playlist's title
     * @return playlist with image or null if it isn't exists
     */

    suspend fun getPlaylistWithImageAsync(title: String): Deferred<PlaylistImage?> =
        coroutineScope { async(Dispatchers.IO) { playlistImageDao.getPlaylistWithImage(title) } }

    /** Removes playlist with its image asynchronously */

    suspend fun removePlaylistWithImageAsync(title: String): Job = coroutineScope {
        launch(Dispatchers.IO) { playlistImageDao.removePlaylistWithImage(title) }
    }

    /** Adds playlist with its image asynchronously */

    suspend fun addPlaylistWithImageAsync(playlist: PlaylistImage): Job = coroutineScope {
        launch(Dispatchers.IO) { playlistImageDao.insertAsync(playlist) }
    }

    /**
     * Gets album with its image asynchronously
     * @param title album's title
     * @return album with image or null if it isn't exists
     */

    suspend fun getAlbumWithImageAsync(title: String): Deferred<AlbumImage?> =
        coroutineScope { async(Dispatchers.IO) { albumImageDao.getAlbumWithImage(title) } }

    /** Removes playlist with its image asynchronously */

    suspend fun removeAlbumWithImageAsync(title: String): Job = coroutineScope {
        launch(Dispatchers.IO) { albumImageDao.removeAlbumWithImage(title) }
    }

    /** Adds playlist with its image asynchronously */

    suspend fun addAlbumWithImageAsync(album: AlbumImage): Job = coroutineScope {
        launch(Dispatchers.IO) { albumImageDao.insertAsync(album) }
    }
}