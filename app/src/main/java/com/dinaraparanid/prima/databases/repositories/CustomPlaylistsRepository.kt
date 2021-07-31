package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack
import kotlinx.coroutines.*

/**
 * Repository for user's playlists
 */

class CustomPlaylistsRepository(context: Context) {
    companion object {
        private const val DATABASE_NAME = "custom_playlists.db"
        private var INSTANCE: CustomPlaylistsRepository? = null

        /**
         * Initialises repository only once
         */

        fun initialize(context: Context) {
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

        val instance: CustomPlaylistsRepository
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
    private val scope: CoroutineScope by lazy {
        (context as MainApplication).mainActivity!!.mainActivityViewModel.viewModelScope
    }

    /**
     * Gets all tracks asynchronously
     * @return all tracks
     */

    val tracksAsync: Deferred<List<CustomPlaylistTrack>>
        get() = scope.async(Dispatchers.IO) { trackDao.getTracksAsync() }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    fun getTrackAsync(path: String): Deferred<CustomPlaylistTrack?> =
        scope.async(Dispatchers.IO) { trackDao.getTrackAsync(path) }

    /**
     * Gets all playlists with some track asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return list of playlists with given track
     * or empty list if there aren't any playlists with such track
     */

    fun getPlaylistsByTrackAsync(path: String): Deferred<List<CustomPlaylist.Entity>> =
        scope.async(Dispatchers.IO) { playlistDao.getPlaylistsByTrackAsync(path) }

    /** Updates track asynchronously */

    fun updateTrack(track: CustomPlaylistTrack): Job =
        scope.launch(Dispatchers.IO) { trackDao.updateTrackAsync(track) }

    /** Adds track asynchronously */

    fun addTrackAsync(track: CustomPlaylistTrack): Deferred<Unit> =
        scope.async(Dispatchers.IO) { trackDao.addTrackAsync(track) }

    /**
     * Removes track with given path and playlistId asynchronously.
     * Since playlists can contain only unique instances of some track,
     * we can simply say that it removes track from playlist with given id
     * @param path path to track (DATA column from MediaStore)
     * @param playlistId id of playlist
     */

    fun removeTrack(path: String, playlistId: Long): Job =
        scope.launch(Dispatchers.IO) { trackDao.removeTrackAsync(path, playlistId) }

    /**
     * Removes all tracks of some playlist asynchronously
     * @param title title of playlist to clear
     */

    fun removeTracksOfPlaylist(title: String): Job =
        scope.launch(Dispatchers.IO) { trackDao.removeTracksOfPlaylistAsync(title) }

    /**
     * Gets all playlists asynchronously
     * @return all playlists
     */

    val playlistsAsync: Deferred<List<CustomPlaylist.Entity>>
        get() = scope.async(Dispatchers.IO) { playlistDao.getPlaylistsAsync() }

    /**
     * Gets playlist by it's title asynchronously
     * @param title title of playlists
     * @return playlist if it exists or null
     */

    fun getPlaylistAsync(title: String): Deferred<CustomPlaylist.Entity?> =
        scope.async(Dispatchers.IO) { playlistDao.getPlaylistAsync(title) }

    /**
     * Updates playlist asynchronously if it's exists
     * @param oldTitle old playlist's title
     * @param newTitle new title for playlist
     */

    fun updatePlaylist(oldTitle: String, newTitle: String): Job = scope.launch(Dispatchers.IO) {
        playlistDao.getPlaylistAsync(oldTitle)?.let { (id) ->
            playlistDao.updatePlaylistAsync(CustomPlaylist.Entity(id, newTitle))
        }
    }

    /**
     * Adds new playlist asynchronously if it wasn't exists
     * @param playlist new playlist
     */

    fun addPlaylistAsync(playlist: CustomPlaylist.Entity): Deferred<Unit> =
        scope.async(Dispatchers.IO) { playlistDao.addPlaylistAsync(playlist) }

    /** Deletes playlist asynchronously */

    fun removePlaylist(title: String): Job = scope.launch(Dispatchers.IO) {
        playlistDao.getPlaylistAsync(title)?.let { playlistDao.removePlaylistAsync(it) }
    }

    /**
     * Gets all playlists with their tracks asynchronously
     * @return all playlists with their tracks
     */

    val playlistsWithTracksAsync: Deferred<List<PlaylistAndTrack>>
        get() = scope.async(Dispatchers.IO) { playlistAndTrackDao.getPlaylistsWithTracksAsync() }

    /**
     * Gets all tracks of playlist by it's title asynchronously
     * @param playlistTitle playlist's title
     * @return tracks of this playlists
     * or empty list if such playlist doesn't exist
     */

    fun getTracksOfPlaylistAsync(playlistTitle: String): Deferred<List<CustomPlaylistTrack>> =
        scope.async(Dispatchers.IO) {
            playlistAndTrackDao.getTracksOfPlaylistAsync(playlistDao.getPlaylistAsync(playlistTitle)!!.id)
        }
}