package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dinaraparanid.prima.databases.databases.FavouriteDatabase
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.favourites.FavouritePlaylist
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Repository for user's favourite tracks and artists */

class FavouriteRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "favourite.db"
        private var INSTANCE: FavouriteRepository? = null
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [FavouriteRepository] is already initialized
         */

        internal fun initialize(context: Context) {
            if (INSTANCE != null) throw IllegalStateException("FavouriteRepository is already initialized")
            INSTANCE = FavouriteRepository(context)
        }

        /**
         * Gets repository's instance without any synchronization
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        private inline val instance: FavouriteRepository
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("FavouriteRepository is not initialized")

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal suspend fun getInstanceSynchronized() = mutex.withLock { instance }
    }

    private val database =
        Room
            .databaseBuilder(
                context.applicationContext,
                FavouriteDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(
                object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) = database.execSQL(
                        "CREATE TABLE favourite_playlists (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, type INTEGER NOT NULL)"
                    )
                }
            )
            .fallbackToDestructiveMigration()
            .build()

    private val tracksDao = database.tracksDao()
    private val artistsDao = database.artistsDao()
    private val playlistsDao = database.playlistsDao()

    /**
     * Gets all favourite tracks asynchronously
     * @return all favourite tracks
     */

    suspend fun getTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTracksAsync() } }

    /**
     * Gets all favourite artists asynchronously
     * @return all favourite artists
     */

    suspend fun getArtistsAsync() =
        coroutineScope { async(Dispatchers.IO) { artistsDao.getArtistsAsync() } }

    /**
     * Gets all favourite playlists asynchronously
     * @return all favourite playlists
     */

    suspend fun getPlaylistsAsync() =
        coroutineScope { async(Dispatchers.IO) { playlistsDao.getPlaylistsAsync() } }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    suspend fun getTrackAsync(path: String) =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTrackAsync(path) } }

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    suspend fun getArtistAsync(name: String) =
        coroutineScope { async(Dispatchers.IO) { artistsDao.getArtistAsync(name) } }

    /**
     * Gets playlist by its title and type asynchronously
     * @param title Playlist's title
     * @param type [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType] as [Int]
     * @return playlist or null if it doesn't exist
     */

    suspend fun getPlaylistAsync(title: String, type: Int) =
        coroutineScope { async(Dispatchers.IO) { playlistsDao.getPlaylistAsync(title, type) } }

    /**
     * Updates track's title, artist and album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     * @param numberInAlbum track's position in album or -1 if no info
     */

    suspend fun updateTrackAsync(
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

    /**
     * Updates playlist's title by its id
     * @param id playlist's id
     * @param title new title
     */

    suspend fun updatePlaylistAsync(id: Long, title: String) =
        coroutineScope { launch(Dispatchers.IO) { playlistsDao.updatePlaylistAsync(id, title) } }

    /** Adds new tracks asynchronously */

    suspend fun addTracksAsync(vararg tracks: FavouriteTrack) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.insertAsync(*tracks) } }

    /** Adds new artists asynchronously */

    suspend fun addArtistsAsync(vararg artists: FavouriteArtist) =
        coroutineScope { launch(Dispatchers.IO) { artistsDao.insertAsync(*artists) } }

    /** Adds new playlists asynchronously */

    suspend fun addPlaylistsAsync(vararg playlists: FavouritePlaylist.Entity) =
        coroutineScope { launch(Dispatchers.IO) { playlistsDao.insertAsync(*playlists) } }

    /** Removes tracks asynchronously */

    suspend fun removeTracksAsync(vararg tracks: FavouriteTrack) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeAsync(*tracks) } }

    /**
     * Removes track by its path
     * @param path track's path
     */

    suspend fun removeTrackAsync(path: String) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeTrack(path) } }

    /** Removes artists asynchronously */

    suspend fun removeArtistsAsync(vararg artists: FavouriteArtist) =
        coroutineScope { launch(Dispatchers.IO) { artistsDao.removeAsync(*artists) } }

    /** Removes playlists asynchronously */

    suspend fun removePlaylistsAsync(vararg playlists: FavouritePlaylist.Entity) =
        coroutineScope { launch(Dispatchers.IO) { playlistsDao.removeAsync(*playlists) } }
}