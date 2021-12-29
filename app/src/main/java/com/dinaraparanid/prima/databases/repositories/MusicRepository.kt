package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.MusicDatabase
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import kotlinx.coroutines.*
import java.util.UUID

@Deprecated("Now using android storage instead of database")
internal class MusicRepository(context: Context) {
    @Deprecated("Now using android storage instead of database")
    internal companion object {
        private const val DATABASE_NAME = "music-database.db"

        private var INSTANCE: MusicRepository? = null

        @Deprecated("Now using android storage instead of database")
        internal fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = MusicRepository(context)
        }

        @Deprecated("Now using android storage instead of database")
        internal val instance: MusicRepository
            @Synchronized
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

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtists() }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun updateArtist(artist: ArtistOld) = coroutineScope {
        launch(Dispatchers.IO) { artistDao.updateAsync(artist) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun addArtist(artist: ArtistOld) = coroutineScope {
        launch(Dispatchers.IO) { artistDao.insertAsync(artist) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistAsync(id: UUID) = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtist(id) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistAsync(name: String) = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtist(name) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistsWithAlbumsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistAndAlbumDao.getArtistsWithAlbums() }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistByAlbumAsync(albumArtistId: UUID) = coroutineScope {
        async(Dispatchers.IO) { artistAndAlbumDao.getArtistByAlbum(albumArtistId) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistsWIthTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { artistAndTrackDao.getArtistsWithTracks() }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistsByTrackAsync(trackId: UUID) = coroutineScope {
        async(Dispatchers.IO) {
            artistAndTrackDao
                .getArtistsWithTracks()
                .filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }
        }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumsAsync() = coroutineScope {
        async(Dispatchers.IO) { albumDao.getAlbums() }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumAsync(id: UUID) = coroutineScope {
        async(Dispatchers.IO) { albumDao.getAlbum(id) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumAsync(title: String) = coroutineScope {
        async(Dispatchers.IO) { albumDao.getAlbum(title) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumsWithTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { albumAndTrackDao.getAlbumsWithTracks() }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumOfTrackAsync(trackAlbumId: UUID) = coroutineScope {
        async(Dispatchers.IO) { albumAndTrackDao.getAlbumByTrack(trackAlbumId) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumsByArtistAsync(artistId: UUID) = coroutineScope {
        async(Dispatchers.IO) { artistAndAlbumDao.getAlbumsByArtist(artistId) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getTracksAsync() = coroutineScope { async(Dispatchers.IO) { trackDao.getTracks() } }

    @Deprecated("Now using android storage instead of database")
    suspend fun updateTrack(track: TrackOld) = coroutineScope {
        launch(Dispatchers.IO) { trackDao.updateAsync(track) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun addTrack(track: TrackOld) = coroutineScope {
        launch(Dispatchers.IO) { trackDao.insertAsync(track) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getTrackAsync(id: UUID) = coroutineScope {
        async(Dispatchers.IO) { trackDao.getTrack(id) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getTrackAsync(title: String) = coroutineScope {
        async(Dispatchers.IO) { trackDao.getTrack(title) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getTracksFromAlbumAsync(albumId: UUID) = coroutineScope {
        async(Dispatchers.IO) { albumAndTrackDao.getTracksFromAlbum(albumId) }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getTracksWithArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistAndTrackDao.getTracksWithArtists() }
    }

    @Deprecated("Now using android storage instead of database")
    suspend fun getTracksByArtistAsync(artistId: UUID) = coroutineScope {
        async(Dispatchers.IO) {
            artistAndTrackDao
                .getArtistsWithTracks()
                .filter { it.artist.artistId == artistId }
        }
    }
}