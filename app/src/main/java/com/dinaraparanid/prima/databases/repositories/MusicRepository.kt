package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.MusicDatabase
import com.dinaraparanid.prima.databases.entities.AlbumOld
import com.dinaraparanid.prima.databases.entities.ArtistOld
import com.dinaraparanid.prima.databases.entities.TrackOld
import com.dinaraparanid.prima.databases.relationships.AlbumAndTrack
import com.dinaraparanid.prima.databases.relationships.ArtistAndAlbum
import com.dinaraparanid.prima.databases.relationships.ArtistWithTracks
import com.dinaraparanid.prima.databases.relationships.TrackWithArtists
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

@Deprecated("Now using android storage instead of database")
internal class MusicRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "music-database.db"

        private var INSTANCE: MusicRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = MusicRepository(context)
        }

        val instance: MusicRepository
            get() = INSTANCE ?: throw UninitializedPropertyAccessException("MusicRepository is not initialized")
    }

    private val database: MusicDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            MusicDatabase::class.java,
            DATABASE_NAME
        )
        .build()

    private val artistDao = database.artistDao()
    private val albumDao = database.albumDao()
    private val trackDao = database.trackDao()
    private val artistAndTrackDao = database.artistAndTrackDao()
    private val albumAndTrackDao = database.albumAndTrackDao()
    private val artistAndAlbumDao = database.artistAndAlbumDao()

    val artistsAsync: Deferred<List<ArtistOld>>
        get() = runBlocking { async { artistDao.getArtists() } }

    fun updateArtist(artist: ArtistOld): Unit =
        runBlocking { launch { artistDao.updateAsync(artist) } }

    fun addArtist(artist: ArtistOld): Unit =
        runBlocking { launch { artistDao.insertAsync(artist) } }

    fun getArtistAsync(id: UUID): Deferred<ArtistOld?> =
        runBlocking { async { artistDao.getArtist(id) } }

    fun getArtistAsync(name: String): Deferred<ArtistOld?> =
        runBlocking { async { artistDao.getArtist(name) } }

    fun getArtistsWithAlbumsAsync(): Deferred<List<ArtistAndAlbum>> =
        runBlocking { async { artistAndAlbumDao.getArtistsWithAlbums() } }

    fun getArtistByAlbumAsync(albumArtistId: UUID): Deferred<ArtistOld?> =
        runBlocking { async { artistAndAlbumDao.getArtistByAlbum(albumArtistId) } }

    fun getArtistsWIthTracksAsync(): Deferred<List<ArtistWithTracks>> =
        runBlocking { async { artistAndTrackDao.getArtistsWithTracks() } }

    fun getArtistsByTrackAsync(trackId: UUID): Deferred<List<ArtistWithTracks>> = runBlocking {
        async {
            artistAndTrackDao
                .getArtistsWithTracks()
                .filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }
        }
    }

    val albumsAsync: Deferred<List<AlbumOld>>
        get() = runBlocking { async { albumDao.getAlbums() } }

    fun getAlbumAsync(id: UUID): Deferred<AlbumOld?> =
        runBlocking { async { albumDao.getAlbum(id) } }

    fun getAlbumAsync(title: String): Deferred<AlbumOld?> =
        runBlocking { async { albumDao.getAlbum(title) } }

    fun getAlbumsWithTracksAsync(): Deferred<List<AlbumAndTrack>> =
        runBlocking { async { albumAndTrackDao.getAlbumsWithTracks() } }

    fun getAlbumOfTrackAsync(trackAlbumId: UUID): Deferred<AlbumOld?> =
        runBlocking { async { albumAndTrackDao.getAlbumByTrack(trackAlbumId) } }

    fun getAlbumsByArtistAsync(artistId: UUID): Deferred<List<AlbumOld>> =
        runBlocking { async { artistAndAlbumDao.getAlbumsByArtist(artistId) } }

    val tracksAsync: Deferred<List<TrackOld>>
        get() = runBlocking { async { trackDao.getTracks() } }

    fun updateTrack(track: TrackOld): Unit =
        runBlocking { launch { trackDao.updateAsync(track) } }

    fun addTrack(track: TrackOld): Unit =
        runBlocking { launch { trackDao.insertAsync(track) } }

    fun getTrackAsync(id: UUID): Deferred<TrackOld?> =
        runBlocking { async { trackDao.getTrack(id) } }

    fun getTrackAsync(title: String): Deferred<TrackOld?> =
        runBlocking { async { trackDao.getTrack(title) } }

    fun getTracksFromAlbumAsync(albumId: UUID): Deferred<List<TrackOld>> =
        runBlocking { async { albumAndTrackDao.getTracksFromAlbum(albumId) } }

    fun getTracksWithArtistsAsync(): Deferred<List<TrackWithArtists>> =
        runBlocking { async { artistAndTrackDao.getTracksWithArtists() } }

    fun getTracksByArtistAsync(artistId: UUID): Deferred<List<ArtistWithTracks>> = runBlocking {
        async {
            artistAndTrackDao
                .getArtistsWithTracks()
                .filter { it.artist.artistId == artistId }
        }
    }
}