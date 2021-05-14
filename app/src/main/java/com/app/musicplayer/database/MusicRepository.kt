package com.app.musicplayer.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.app.musicplayer.core.Album
import com.app.musicplayer.core.Artist
import com.app.musicplayer.core.Track
import java.util.UUID
import java.util.concurrent.Executors

class MusicRepository private constructor(context: Context) {
    private val database: MusicDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            MusicDatabase::class.java,
            DATABASE_NAME
        )
        .build().apply {
            Log.d("DB PATH", openHelper.writableDatabase.path)
        }

    private val artistDao = database.artistDao()
    private val albumDao = database.albumDao()
    private val trackDao = database.trackDao()
    private val artistAndTrackDao = database.artistAndTrackDao()
    private val albumAndTrackDao = database.albumAndTrackDao()
    private val artistAndAlbumDao = database.artistAndAlbumDao()
    private val executor = Executors.newSingleThreadExecutor()

    val artists: LiveData<List<Artist>> get() = artistDao.getArtists()
    fun getArtist(id: UUID) = artistDao.getArtist(id)
    fun getArtist(name: String) = artistDao.getArtist(name)
    fun getArtistsWithAlbums() = artistAndAlbumDao.getArtistsWithAlbums()
    fun getArtistByAlbum(albumArtistId: UUID) = artistAndAlbumDao.getArtistByAlbum(albumArtistId)
    fun getArtistsWIthTracks() = artistAndTrackDao.getArtistsWithTracks()
    fun getArtistsByTrack(trackId: UUID) = artistAndTrackDao
        .getArtistsWithTracks()
        .value
        ?.filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }

    val albums: LiveData<List<Album>> get() = albumDao.getAlbums()
    fun getAlbum(id: UUID) = albumDao.getAlbum(id)
    fun getAlbum(title: String) = albumDao.getAlbum(title)
    fun getAlbumsWithTracks() = albumAndTrackDao.getAlbumsWithTracks()
    fun getAlbumOfTrack(trackAlbumId: UUID) = albumAndTrackDao.getAlbumByTrack(trackAlbumId)
    fun getAlbumsByArtist(artistId: UUID) = artistAndAlbumDao.getAlbumsByArtist(artistId)

    val tracks: LiveData<List<Track>> get() = trackDao.getTracks()
    fun updateTrack(track: Track) = executor.execute { trackDao.updateTrack(track) }
    fun addTrack(track: Track) = executor.execute { trackDao.addTrack(track) }
    fun getTrack(id: UUID) = trackDao.getTrack(id)
    fun getTrack(title: String) = trackDao.getTrack(title)
    fun getTracksFromAlbum(albumId: UUID) = albumAndTrackDao.getTracksFromAlbum(albumId)
    fun getTracksWithArtists() = artistAndTrackDao.getTracksWithArtists()
    fun getTracksByArtist(artistId: UUID) = artistAndTrackDao
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

        fun getInstance() =
            INSTANCE ?: throw IllegalStateException("MusicRepository is not initialized")
    }
}