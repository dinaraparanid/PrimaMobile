package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
                ?: throw IllegalStateException("CustomPlaylistsRepository is not initialized")
    }

    private val database = Room.databaseBuilder(
        context.applicationContext,
        CustomPlaylistsDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val trackDao = database.customPlaylistTrackDao()
    private val playlistDao = database.customPlaylistDao()
    private val playlistAndTrackDao = database.customPlaylistAndTrackDao()

    val tracksAsync: Deferred<List<CustomPlaylistTrack>>
        get() = runBlocking { async { trackDao.getTracks() } }

    fun getTrackAsync(id: Long): Deferred<CustomPlaylistTrack?> =
        runBlocking { async { trackDao.getTrack(id) } }

    fun getPlaylistsByTrackAsync(path: String): Deferred<List<CustomPlaylist.Entity>> =
        runBlocking { async { playlistDao.getPlaylistsByTrack(path) } }

    fun updateTrack(track: CustomPlaylistTrack): Unit =
        runBlocking { launch { trackDao.updateTrack(track) } }

    fun addTrack(track: CustomPlaylistTrack): Unit =
        runBlocking { launch { trackDao.addTrack(track) } }

    fun removeTrack(path: String, playlistId: Long): Unit =
        runBlocking { launch { trackDao.removeTrack(path, playlistId) } }

    fun removeTracksOfPlaylist(title: String): Unit =
        runBlocking { launch { trackDao.removeTracksOfPlaylist(title) } }

    val playlistsAsync: Deferred<List<CustomPlaylist.Entity>>
        get() = runBlocking { async { playlistDao.getPlaylists() } }

    fun getPlaylistAsync(title: String): Deferred<CustomPlaylist.Entity?> =
        runBlocking { async { playlistDao.getPlaylist(title) } }

    fun updatePlaylist(oldTitle: String, newTitle: String): Unit =
        runBlocking {
            launch {
                playlistDao.getPlaylist(oldTitle)?.let { (id) ->
                    playlistDao.updatePlaylist(CustomPlaylist.Entity(id, newTitle))
                }
            }
        }

    fun addPlaylist(playlist: CustomPlaylist.Entity): Unit =
        runBlocking { launch { playlistDao.addPlaylist(playlist) } }

    fun removePlaylist(title: String): Unit =
        runBlocking {
            launch {
                playlistDao.getPlaylist(title)?.let { playlistDao.removePlaylist(it) }
            }
        }

    val playlistsWithTracksAsync: Deferred<List<PlaylistAndTrack>>
        get() = runBlocking { async { playlistAndTrackDao.getPlaylistsWithTracks() } }

    fun getPlaylistByTrackAsync(albumTitle: String): Deferred<CustomPlaylist.Entity?> =
        runBlocking { async { playlistAndTrackDao.getPlaylistByTrack(albumTitle) } }

    fun getTracksOfPlaylistAsync(playlistTitle: String): Deferred<List<CustomPlaylistTrack>> =
        runBlocking {
            async {
                playlistAndTrackDao.getTracksOfPlaylist(playlistDao.getPlaylist(playlistTitle)!!.id)
            }
        }
}