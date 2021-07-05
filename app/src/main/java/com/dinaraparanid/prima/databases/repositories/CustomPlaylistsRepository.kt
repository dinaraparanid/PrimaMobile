package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

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
    private val executor = Executors.newSingleThreadExecutor()

    val tracks: List<CustomPlaylistTrack> get() = runBlocking { trackDao.getTracks() }
    fun getTrack(id: Long): CustomPlaylistTrack? = runBlocking { trackDao.getTrack(id) }

    fun updateTrack(track: CustomPlaylistTrack): Unit =
        executor.execute { runBlocking { trackDao.updateTrack(track) } }

    fun addTrack(track: CustomPlaylistTrack): Unit =
        executor.execute { runBlocking { trackDao.addTrack(track) } }

    fun removeTrack(path: String): Unit =
        executor.execute { runBlocking { trackDao.removeTrack(path) } }

    fun removeTracksOfPlaylist(title: String): Unit =
        executor.execute { runBlocking { trackDao.removeTracksOfPlaylist(title) } }

    val playlists: List<CustomPlaylist.Entity> get() = runBlocking { playlistDao.getPlaylists() }

    fun getPlaylist(title: String): CustomPlaylist.Entity? =
        runBlocking { playlistDao.getPlaylist(title) }

    fun updatePlaylist(oldTitle: String, newTitle: String): Unit =
        executor.execute { runBlocking { playlistDao.updatePlaylist(oldTitle, newTitle) } }

    fun addPlaylist(playlist: CustomPlaylist.Entity): Unit =
        executor.execute { runBlocking { playlistDao.addPlaylist(playlist) } }

    fun removePlaylist(playlist: CustomPlaylist.Entity): Unit =
        executor.execute { runBlocking { playlistDao.removePlaylist(playlist) } }

    val playlistsWithTracks: List<PlaylistAndTrack>
        get() = runBlocking { playlistAndTrackDao.getPlaylistsWithTracks() }

    fun getPlaylistByTrack(albumTitle: String): CustomPlaylist.Entity? =
        runBlocking { playlistAndTrackDao.getPlaylistByTrack(albumTitle) }

    fun getTracksOfPlaylist(playlistTitle: String): List<CustomPlaylistTrack> =
        runBlocking { playlistAndTrackDao.getTracksOfPlaylist(playlistTitle) }
}