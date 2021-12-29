package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.FavouriteDatabase
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.favourites.FavouritePlaylist
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.*

/**
 * Repository for user's favourite tracks and artists
 */

class FavouriteRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "favourite.db"

        private var INSTANCE: FavouriteRepository? = null

        /**
         * Initialises repository only once
         */

        internal fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = FavouriteRepository(context)
        }

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal val instance: FavouriteRepository
            @Synchronized
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("FavouriteRepository is not initialized")
    }

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            FavouriteDatabase::class.java,
            DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()

    private val trackDao = database.trackDao()
    private val artistDao = database.artistDao()
    private val playlistDao = database.playlistDao()

    /**
     * Gets all favourite tracks asynchronously
     * @return all favourite tracks
     */

    suspend fun getTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { trackDao.getTracksAsync() }
    }

    /**
     * Gets all favourite artists asynchronously
     * @return all favourite artists
     */

    suspend fun getArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtistsAsync() }
    }

    /**
     * Gets all favourite playlists asynchronously
     * @return all favourite playlists
     */

    suspend fun getPlaylistsAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistDao.getPlaylistsAsync() }
    }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    suspend fun getTrackAsync(path: String) = coroutineScope {
        async(Dispatchers.IO) { trackDao.getTrackAsync(path) }
    }

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    suspend fun getArtistAsync(name: String) = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtistAsync(name) }
    }

    /**
     * Gets playlist by its title and type asynchronously
     * @param title Playlist's title
     * @param type [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType] as [Int]
     * @return playlist or null if it doesn't exist
     */

    suspend fun getPlaylistAsync(title: String, type: Int) = coroutineScope {
        async(Dispatchers.IO) { playlistDao.getPlaylistAsync(title, type) }
    }

    /**
     * Updates track's title, artist and album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     */

    suspend fun updateTrackAsync(path: String, title: String, artist: String, album: String) =
        coroutineScope { launch(Dispatchers.IO) { trackDao.updateTrackAsync(path, title, artist, album) } }

    /**
     * Updates playlist's title by its id
     * @param id playlist's id
     * @param title new title
     */

    suspend fun updatePlaylistAsync(id: Long, title: String) = coroutineScope {
        launch(Dispatchers.IO) { playlistDao.updatePlaylistAsync(id, title) }
    }

    /** Adds tracks asynchronously */

    suspend fun addTrackAsync(track: FavouriteTrack) = coroutineScope {
        launch(Dispatchers.IO) { trackDao.insertAsync(track) }
    }

    /** Adds new artist asynchronously */

    suspend fun addArtistAsync(artist: FavouriteArtist) = coroutineScope {
        launch(Dispatchers.IO) { artistDao.insertAsync(artist) }
    }

    /** Adds new playlist asynchronously */

    suspend fun addPlaylistAsync(playlist: FavouritePlaylist.Entity) = coroutineScope {
        launch(Dispatchers.IO) { playlistDao.insertAsync(playlist) }
    }

    /** Removes track asynchronously */

    suspend fun removeTrackAsync(track: FavouriteTrack) = coroutineScope {
        launch(Dispatchers.IO) { trackDao.removeAsync(track) }
    }

    /** Removes artist asynchronously */

    suspend fun removeArtistAsync(artist: FavouriteArtist) = coroutineScope {
        launch(Dispatchers.IO) { artistDao.removeAsync(artist) }
    }

    /** Removes playlist asynchronously */

    suspend fun removePlaylistAsync(playlist: FavouritePlaylist.Entity) = coroutineScope {
        launch(Dispatchers.IO) { playlistDao.removeAsync(playlist) }
    }
}