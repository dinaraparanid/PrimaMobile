package com.dinaraparanid.prima.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.dinaraparanid.prima.core.AlbumOld
import com.dinaraparanid.prima.core.ArtistOld
import com.dinaraparanid.prima.core.TrackOld
import com.dinaraparanid.prima.database.relationships.AlbumAndTrack
import com.dinaraparanid.prima.database.relationships.ArtistAndAlbum
import com.dinaraparanid.prima.database.relationships.ArtistWithTracks
import com.dinaraparanid.prima.database.relationships.TrackWithArtists
import java.util.UUID
import java.util.concurrent.Executors

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
    private val executor = Executors.newSingleThreadExecutor()

    val artists: LiveData<List<ArtistOld>> get() = artistDao.getArtists()
    fun updateArtist(artist: ArtistOld): Unit = executor.execute { artistDao.updateArtist(artist) }
    fun addArtist(artist: ArtistOld): Unit = executor.execute { artistDao.addArtist(artist) }
    fun getArtist(id: UUID): LiveData<ArtistOld?> = artistDao.getArtist(id)
    fun getArtist(name: String): LiveData<ArtistOld?> = artistDao.getArtist(name)
    fun getArtistsWithAlbums(): LiveData<List<ArtistAndAlbum>> =
        artistAndAlbumDao.getArtistsWithAlbums()

    fun getArtistByAlbum(albumArtistId: UUID): LiveData<ArtistOld?> =
        artistAndAlbumDao.getArtistByAlbum(albumArtistId)

    fun getArtistsWIthTracks(): LiveData<List<ArtistWithTracks>> =
        artistAndTrackDao.getArtistsWithTracks()

    fun getArtistsByTrack(trackId: UUID): List<ArtistWithTracks>? = artistAndTrackDao
        .getArtistsWithTracks()
        .value
        ?.filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }

    val albums: LiveData<List<AlbumOld>> get() = albumDao.getAlbums()
    fun getAlbum(id: UUID): LiveData<AlbumOld?> = albumDao.getAlbum(id)
    fun getAlbum(title: String): LiveData<AlbumOld?> = albumDao.getAlbum(title)
    fun getAlbumsWithTracks(): LiveData<List<AlbumAndTrack>> =
        albumAndTrackDao.getAlbumsWithTracks()

    fun getAlbumOfTrack(trackAlbumId: UUID): LiveData<AlbumOld?> =
        albumAndTrackDao.getAlbumByTrack(trackAlbumId)

    fun getAlbumsByArtist(artistId: UUID): LiveData<List<AlbumOld>> =
        artistAndAlbumDao.getAlbumsByArtist(artistId)

    val tracks: LiveData<List<TrackOld>> get() = trackDao.getTracks()
    fun updateTrack(track: TrackOld): Unit = executor.execute { trackDao.updateTrack(track) }
    fun addTrack(track: TrackOld): Unit = executor.execute { trackDao.addTrack(track) }
    fun getTrack(id: UUID): LiveData<TrackOld?> = trackDao.getTrack(id)
    fun getTrack(title: String): LiveData<TrackOld?> = trackDao.getTrack(title)
    fun getTracksFromAlbum(albumId: UUID): LiveData<List<TrackOld>> =
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