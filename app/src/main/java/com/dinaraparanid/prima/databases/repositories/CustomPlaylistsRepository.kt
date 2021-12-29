package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import kotlinx.coroutines.*

/**
 * Repository for user's playlists
 */

class CustomPlaylistsRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "custom_playlists.db"
        private var INSTANCE: CustomPlaylistsRepository? = null

        /**
         * Initialises repository only once
         */

        internal fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = CustomPlaylistsRepository(context)
        }

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal val instance: CustomPlaylistsRepository
            @Synchronized
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("CustomPlaylistsRepository is not initialized")
    }

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            CustomPlaylistsDatabase::class.java,
            DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()

    private val trackDao = database.customPlaylistTrackDao()
    private val playlistDao = database.customPlaylistDao()
    private val playlistAndTrackDao = database.customPlaylistAndTrackDao()

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    suspend fun getTrackAsync(path: String) =
        coroutineScope { async(Dispatchers.IO) { trackDao.getTrackAsync(path) } }

    /**
     * Gets all playlists with some track asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return list of playlists with given track
     * or empty list if there aren't any playlists with such track
     */

    suspend fun getPlaylistsByTrackAsync(path: String) =
        coroutineScope { async(Dispatchers.IO) { playlistDao.getPlaylistsByTrackAsync(path) } }

    /** Updates track asynchronously */

    suspend fun updateTrackAsync(track: CustomPlaylistTrack) =
        coroutineScope { launch(Dispatchers.IO) { trackDao.updateAsync(track) } }

    /**
     * Updates track's title, artist and album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     */

    suspend fun updateTrackAsync(path: String, title: String, artist: String, album: String) =
        coroutineScope { launch(Dispatchers.IO) { trackDao.updateTrackAsync(path, title, artist, album) } }

    /** Adds track asynchronously */

    suspend fun addTrackAsync(track: CustomPlaylistTrack) =
        coroutineScope { launch(Dispatchers.IO) { trackDao.insertAsync(track) } }

    /**
     * Removes track with given path and playlistId asynchronously.
     * Since playlists can contain only unique instances of some track,
     * we can simply say that it removes track from playlist with given id
     * @param path path to track (DATA column from MediaStore)
     * @param playlistId id of playlist
     */

    suspend fun removeTrackAsync(path: String, playlistId: Long) =
        coroutineScope { launch(Dispatchers.IO) { trackDao.removeTrackAsync(path, playlistId) } }

    /**
     * Removes all tracks of some playlist asynchronously
     * @param title title of playlist to clear
     */

    suspend fun removeTracksOfPlaylistAsync(title: String) =
        coroutineScope { launch(Dispatchers.IO) { trackDao.removeTracksOfPlaylistAsync(title) } }

    /**
     * Gets all playlists asynchronously
     * @return all playlists
     */

    suspend fun getPlaylistsAsync() =
        coroutineScope { async(Dispatchers.IO) { playlistDao.getPlaylistsAsync() } }

    /**
     * Gets playlist by it's title asynchronously
     * @param title title of playlists
     * @return playlist if it exists or null
     */

    suspend fun getPlaylistAsync(title: String) =
        coroutineScope { async(Dispatchers.IO) { playlistDao.getPlaylistAsync(title) } }

    /**
     * Updates playlist asynchronously if it's exists
     * @param oldTitle old playlist's title
     * @param newTitle new title for playlist
     */

    suspend fun updatePlaylistAsync(oldTitle: String, newTitle: String) = coroutineScope {
        launch(Dispatchers.IO) {
            playlistDao.getPlaylistAsync(oldTitle)?.let { (id) ->
                playlistDao.updateAsync(CustomPlaylist.Entity(id, newTitle))
            }
        }
    }

    /**
     * Adds new playlist asynchronously if it wasn't exists
     * @param playlist new playlist
     */

    suspend fun addPlaylistAsync(playlist: CustomPlaylist.Entity) =
        coroutineScope { launch(Dispatchers.IO) { playlistDao.insertAsync(playlist) } }

    /** Deletes playlist asynchronously */

    suspend fun removePlaylistAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) {
            playlistDao.getPlaylistAsync(title)?.let { playlistDao.removeAsync(it) }
        }
    }

    /**
     * Gets all playlists with their tracks asynchronously
     * @return all playlists with their tracks
     */

    suspend fun getPlaylistsWithTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { playlistAndTrackDao.getPlaylistsWithTracksAsync() } }

    /**
     * Gets all tracks of playlist by it's title asynchronously
     * @param playlistTitle playlist's title
     * @return tracks of this playlists
     * or empty list if such playlist doesn't exist
     */

    suspend fun getTracksOfPlaylistAsync(playlistTitle: String) = coroutineScope {
        async(Dispatchers.IO) {
            playlistAndTrackDao.getTracksOfPlaylistAsync(
                playlistDao.getPlaylistAsync(playlistTitle)!!.id
            )
        }
    }

    /**
     * Gets 1-st track of playlist asynchronously
     * @param playlistTitle playlist's title
     * @return 1-st track of this playlists
     * or null if such playlist doesn't exist or empty
     */

    suspend fun getFirstTrackOfPlaylistAsync(playlistTitle: String) = coroutineScope {
        async(Dispatchers.IO) {
            playlistAndTrackDao.getFirstTrackOfPlaylistAsync(
                playlistDao.getPlaylistAsync(playlistTitle)!!.id
            )
        }
    }
}