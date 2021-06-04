package com.dinaraparanid.prima.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.dinaraparanid.prima.core.Album
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.relationships.AlbumAndTrack
import com.dinaraparanid.prima.database.relationships.ArtistAndAlbum
import com.dinaraparanid.prima.database.relationships.ArtistWithTracks
import com.dinaraparanid.prima.database.relationships.TrackWithArtists
import java.util.UUID
import java.util.concurrent.Executors

class MusicRepository private constructor(context: Context) {
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
    private val executor = Executors.newSingleThreadExecutor()

    val artists: LiveData<List<Artist>> get() = artistDao.getArtists()
    fun updateArtist(artist: Artist): Unit = executor.execute { artistDao.updateArtist(artist) }
    fun addArtist(artist: Artist): Unit = executor.execute { artistDao.addArtist(artist) }
    fun getArtist(id: UUID): LiveData<Artist?> = artistDao.getArtist(id)
    fun getArtist(name: String): LiveData<Artist?> = artistDao.getArtist(name)
    fun getArtistsWithAlbums(): LiveData<List<ArtistAndAlbum>> =
        artistAndAlbumDao.getArtistsWithAlbums()

    fun getArtistByAlbum(albumArtistId: UUID): LiveData<Artist?> =
        artistAndAlbumDao.getArtistByAlbum(albumArtistId)

    fun getArtistsWIthTracks(): LiveData<List<ArtistWithTracks>> =
        artistAndTrackDao.getArtistsWithTracks()

    fun getArtistsByTrack(trackId: UUID): List<ArtistWithTracks>? = artistAndTrackDao
        .getArtistsWithTracks()
        .value
        ?.filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }

    val albums: LiveData<List<Album>> get() = albumDao.getAlbums()
    fun getAlbum(id: UUID): LiveData<Album?> = albumDao.getAlbum(id)
    fun getAlbum(title: String): LiveData<Album?> = albumDao.getAlbum(title)
    fun getAlbumsWithTracks(): LiveData<List<AlbumAndTrack>> =
        albumAndTrackDao.getAlbumsWithTracks()

    fun getAlbumOfTrack(trackAlbumId: UUID): LiveData<Album?> =
        albumAndTrackDao.getAlbumByTrack(trackAlbumId)

    fun getAlbumsByArtist(artistId: UUID): LiveData<List<Album>> =
        artistAndAlbumDao.getAlbumsByArtist(artistId)

    val tracks: LiveData<List<Track>> get() = trackDao.getTracks()
    fun updateTrack(track: Track): Unit = executor.execute { trackDao.updateTrack(track) }
    fun addTrack(track: Track): Unit = executor.execute { trackDao.addTrack(track) }
    fun getTrack(id: UUID): LiveData<Track?> = trackDao.getTrack(id)
    fun getTrack(title: String): LiveData<Track?> = trackDao.getTrack(title)
    fun getTracksFromAlbum(albumId: UUID): LiveData<List<Track>> =
        albumAndTrackDao.getTracksFromAlbum(albumId)

    fun getTracksWithArtists(): LiveData<List<TrackWithArtists>> =
        artistAndTrackDao.getTracksWithArtists()

    fun getTracksByArtist(artistId: UUID): List<ArtistWithTracks>? = artistAndTrackDao
        .getArtistsWithTracks()
        .value
        ?.filter { it.artist.artistId == artistId }

    companion object {
        private const val DATABASE_NAME = "music-database.db"

        private var INSTANCE: MusicRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = MusicRepository(context)
        }

        fun getInstance(): MusicRepository =
            INSTANCE ?: throw IllegalStateException("MusicRepository is not initialized")
    }
}