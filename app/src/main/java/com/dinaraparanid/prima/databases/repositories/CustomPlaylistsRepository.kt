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

class CustomPlaylistsRepository(context: Context) {
    companion object {
        private const val DATABASE_NAME = "custom_playlists.db"
        private var INSTANCE: CustomPlaylistsRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = CustomPlaylistsRepository(context)
        }

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

    val tracksAsync: Deferred<List<CustomPlaylistTrack>>
        get() = scope.async(Dispatchers.IO) { trackDao.getTracks() }

    fun getTrackAsync(path: String): Deferred<CustomPlaylistTrack?> =
        scope.async(Dispatchers.IO) { trackDao.getTrack(path) }

    fun getPlaylistsByTrackAsync(path: String): Deferred<List<CustomPlaylist.Entity>> =
        scope.async(Dispatchers.IO) { playlistDao.getPlaylistsByTrack(path) }

    fun updateTrack(track: CustomPlaylistTrack): Job =
        scope.launch(Dispatchers.IO) { trackDao.updateTrack(track) }

    fun addTrack(track: CustomPlaylistTrack): Job =
        scope.launch(Dispatchers.IO) { trackDao.addTrack(track) }

    fun removeTrack(path: String, playlistId: Long): Job =
        scope.launch(Dispatchers.IO) { trackDao.removeTrack(path, playlistId) }

    fun removeTracksOfPlaylist(title: String): Job =
        scope.launch(Dispatchers.IO) { trackDao.removeTracksOfPlaylist(title) }

    val playlistsAsync: Deferred<List<CustomPlaylist.Entity>>
        get() = scope.async(Dispatchers.IO) { playlistDao.getPlaylists() }

    fun getPlaylistAsync(title: String): Deferred<CustomPlaylist.Entity?> =
        scope.async(Dispatchers.IO) { playlistDao.getPlaylist(title) }

    fun updatePlaylist(oldTitle: String, newTitle: String): Job = scope.launch(Dispatchers.IO) {
        playlistDao.getPlaylist(oldTitle)?.let { (id) ->
            playlistDao.updatePlaylist(CustomPlaylist.Entity(id, newTitle))
        }
    }

    fun addPlaylist(playlist: CustomPlaylist.Entity): Job =
        scope.launch(Dispatchers.IO) { playlistDao.addPlaylist(playlist) }

    fun removePlaylist(title: String): Job = scope.launch(Dispatchers.IO) {
        playlistDao.getPlaylist(title)?.let { playlistDao.removePlaylist(it) }
    }

    val playlistsWithTracksAsync: Deferred<List<PlaylistAndTrack>>
        get() = scope.async(Dispatchers.IO) { playlistAndTrackDao.getPlaylistsWithTracks() }

    fun getPlaylistByTrackAsync(albumTitle: String): Deferred<CustomPlaylist.Entity?> =
        scope.async(Dispatchers.IO) { playlistAndTrackDao.getPlaylistByTrack(albumTitle) }

    fun getTracksOfPlaylistAsync(playlistTitle: String): Deferred<List<CustomPlaylistTrack>> =
        scope.async(Dispatchers.IO) {
            playlistAndTrackDao.getTracksOfPlaylist(playlistDao.getPlaylist(playlistTitle)!!.id)
        }
}