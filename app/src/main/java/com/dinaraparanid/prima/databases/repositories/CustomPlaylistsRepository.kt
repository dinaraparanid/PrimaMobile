package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.relationships.PlaylistAndTrack
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

    val tracks: List<CustomPlaylistTrack> get() = runBlocking { trackDao.getTracks() }
    fun getTrack(id: Long): CustomPlaylistTrack? = runBlocking { trackDao.getTrack(id) }
    fun updateTrack(track: CustomPlaylistTrack): Unit = runBlocking { trackDao.updateTrack(track) }
    fun addTrack(track: CustomPlaylistTrack): Unit = runBlocking { trackDao.addTrack(track) }
    fun removeTrack(track: CustomPlaylistTrack): Unit = runBlocking { trackDao.removeTrack(track) }
    fun removeTracksOfPlaylist(title: String): Unit =
        runBlocking { trackDao.removeTracksOfPlaylist(title) }

    val playlists: List<CustomPlaylist.Entity> get() = runBlocking { playlistDao.getPlaylists() }

    fun getPlaylist(title: String): CustomPlaylist.Entity? =
        runBlocking { playlistDao.getPlaylist(title) }

    fun updatePlaylist(playlist: CustomPlaylist.Entity): Unit =
        runBlocking { playlistDao.updatePlaylist(playlist) }

    fun addPlaylist(playlist: CustomPlaylist.Entity): Unit =
        runBlocking { playlistDao.addPlaylist(playlist) }

    fun removePlaylist(playlist: CustomPlaylist.Entity): Unit =
        runBlocking { playlistDao.removePlaylist(playlist) }

    val playlistsWithTracks: List<PlaylistAndTrack>
        get() = runBlocking { playlistAndTrackDao.getPlaylistsWithTracks() }

    fun getPlaylistByTrack(albumTitle: String): CustomPlaylist.Entity? =
        runBlocking { playlistAndTrackDao.getPlaylistByTrack(albumTitle) }

    fun getTracksOfPlaylist(playlistTitle: String): List<CustomPlaylistTrack> =
        runBlocking { playlistAndTrackDao.getTracksOfPlaylist(playlistTitle) }
}