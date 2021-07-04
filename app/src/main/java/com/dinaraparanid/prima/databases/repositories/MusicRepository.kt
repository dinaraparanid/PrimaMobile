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
import kotlinx.coroutines.runBlocking
import java.util.UUID

@Deprecated("Now using android storage instead of database")
class MusicRepository(context: Context) {
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

    val artists: List<ArtistOld> get() = runBlocking { artistDao.getArtists() }
    fun updateArtist(artist: ArtistOld): Unit = runBlocking { artistDao.updateArtist(artist) }
    fun addArtist(artist: ArtistOld): Unit = runBlocking { artistDao.addArtist(artist) }
    fun getArtist(id: UUID): ArtistOld? = runBlocking { artistDao.getArtist(id) }
    fun getArtist(name: String): ArtistOld? = runBlocking { artistDao.getArtist(name) }
    fun getArtistsWithAlbums(): List<ArtistAndAlbum> =
        runBlocking { artistAndAlbumDao.getArtistsWithAlbums() }

    fun getArtistByAlbum(albumArtistId: UUID): ArtistOld? =
        runBlocking { artistAndAlbumDao.getArtistByAlbum(albumArtistId) }

    fun getArtistsWIthTracks(): List<ArtistWithTracks> =
        runBlocking { artistAndTrackDao.getArtistsWithTracks() }

    fun getArtistsByTrack(trackId: UUID): List<ArtistWithTracks> =
        runBlocking { artistAndTrackDao.getArtistsWithTracks() }
            .filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }

    val albums: List<AlbumOld> get() = runBlocking { albumDao.getAlbums() }
    fun getAlbum(id: UUID): AlbumOld? = runBlocking { albumDao.getAlbum(id) }
    fun getAlbum(title: String): AlbumOld? = runBlocking { albumDao.getAlbum(title) }
    fun getAlbumsWithTracks(): List<AlbumAndTrack> =
        runBlocking { albumAndTrackDao.getAlbumsWithTracks() }

    fun getAlbumOfTrack(trackAlbumId: UUID): AlbumOld? =
        runBlocking { albumAndTrackDao.getAlbumByTrack(trackAlbumId) }

    fun getAlbumsByArtist(artistId: UUID): List<AlbumOld> =
        runBlocking { artistAndAlbumDao.getAlbumsByArtist(artistId) }

    val tracks: List<TrackOld> get() = runBlocking { trackDao.getTracks() }
    fun updateTrack(track: TrackOld): Unit = runBlocking { trackDao.updateTrack(track) }
    fun addTrack(track: TrackOld): Unit = runBlocking { trackDao.addTrack(track) }
    fun getTrack(id: UUID): TrackOld? = runBlocking { trackDao.getTrack(id) }
    fun getTrack(title: String): TrackOld? = runBlocking { trackDao.getTrack(title) }
    fun getTracksFromAlbum(albumId: UUID): List<TrackOld> =
        runBlocking { albumAndTrackDao.getTracksFromAlbum(albumId) }

    fun getTracksWithArtists(): List<TrackWithArtists> =
        runBlocking { artistAndTrackDao.getTracksWithArtists() }

    fun getTracksByArtist(artistId: UUID): List<ArtistWithTracks> =
        runBlocking { artistAndTrackDao.getArtistsWithTracks() }
            .filter { it.artist.artistId == artistId }

    companion object {
        private const val DATABASE_NAME = "music-database.db"

        private var INSTANCE: MusicRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = MusicRepository(context)
        }

        val instance: MusicRepository
            get() = INSTANCE ?: throw IllegalStateException("MusicRepository is not initialized")
    }
}