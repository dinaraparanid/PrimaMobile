package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.MusicDatabase
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.ArtistAndAlbum
import com.dinaraparanid.prima.databases.relationships.ArtistWithTracks
import com.dinaraparanid.prima.databases.relationships.TrackWithArtists
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import com.dinaraparanid.prima.databases.repositories.CoversRepository.Companion.initialize
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Old repository for all music data
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
internal class MusicRepository(context: Context) {
    @Deprecated("Now using android MediaStore instead of database")
    internal companion object {
        private const val DATABASE_NAME = "music-database.db"

        @JvmStatic
        private var INSTANCE: MusicRepository? = null

        @JvmStatic
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [MusicRepository] is already initialized
         */

        @JvmStatic
        @Deprecated("Now using android MediaStore instead of database")
        internal fun initialize(context: Context) {
            if (INSTANCE != null) throw IllegalStateException("MusicRepository is already initialized")
            INSTANCE = MusicRepository(context)
        }

        /**
         * Gets repository's instance without any synchronization
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        @Deprecated("Now using android MediaStore instead of database")
        private inline val instance: MusicRepository
            @JvmStatic
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("MusicRepository is not initialized")

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        @JvmStatic
        @Deprecated("Now using android MediaStore instead of database")
        internal suspend fun getInstanceSynchronized() = mutex.withLock { instance }
    }

    private val database: MusicDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            MusicDatabase::class.java,
            DATABASE_NAME
        )
        .build()

    private val artistsDao = database.artistsDao()
    private val albumsDao = database.albumsDao()
    private val tracksDao = database.tracksDao()
    private val artistsAndTracksDao = database.artistsAndTracksDao()
    private val albumsAndTracksDao = database.albumsAndTracksDao()
    private val artistsAndAlbumsDao = database.artistsAndAlbumsDao()

    /**
     * Gets all artists
     * @return list with all artists
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getArtists() }
    }

    @Deprecated("Now using android MediaStore instead of database")
    private suspend fun <T> addEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope { launch(Dispatchers.IO) { dao.insertAsync(*entities) } }

    @Deprecated("Now using android MediaStore instead of database")
    private suspend fun <T> removeEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope { launch(Dispatchers.IO) { dao.removeAsync(*entities) } }

    @Deprecated("Now using android MediaStore instead of database")
    private suspend fun <T> updateEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope { launch(Dispatchers.IO) { dao.updateAsync(*entities) } }

    /**
     * Updates artists
     * @param artists [ArtistOld] to change
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun updateArtists(vararg artists: ArtistOld) = updateEntitiesAsync(artistsDao, *artists)

    /**
     * Adds new artists
     * @param artists [ArtistOld] to add
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun addArtists(vararg artists: ArtistOld) = addEntitiesAsync(artistsDao, *artists)

    /**
     * Gets artist by his id or null if he wasn't found
     * @param id id of artist
     * @return found artist or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistAsync(id: UUID) = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getArtist(id) }
    }

    /**
     * Gets artist by his name or null if he wasn't found
     * @param name name of album
     * @return found artist or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistAsync(name: String) = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getArtist(name) }
    }

    /**
     * Gets all artist-album relationships
     * @return list of all [ArtistAndAlbum] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsWithAlbumsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsAndAlbumsDao.getArtistsWithAlbums() }
    }

    /**
     * Gets artist by album or null if there is no such artist
     * @param albumArtistId artist's id of album
     * @return found artist or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistByAlbumAsync(albumArtistId: UUID) = coroutineScope {
        async(Dispatchers.IO) { artistsAndAlbumsDao.getArtistByAlbum(albumArtistId) }
    }

    /**
     * Gets all artist-tracks relationships
     * @return list of all [ArtistWithTracks] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsWIthTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsAndTracksDao.getArtistsWithTracks() }
    }

    /**
     * Gets all artists by track
     * @return list of all [ArtistWithTracks] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsByTrackAsync(trackId: UUID) = coroutineScope {
        async(Dispatchers.IO) {
            artistsAndTracksDao
                .getArtistsWithTracks()
                .filter { it.tracks.find { (curTrackId) -> curTrackId == trackId } != null }
        }
    }

    /**
     * Gets all albums
     * @return list with all albums
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumsAsync() = coroutineScope {
        async(Dispatchers.IO) { albumsDao.getAlbums() }
    }

    /**
     * Gets album by its id or null if it wasn't found
     * @param id id of album
     * @return found album or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumAsync(id: UUID) = coroutineScope {
        async(Dispatchers.IO) { albumsDao.getAlbum(id) }
    }

    /**
     * Gets album by its title or null if it wasn't found
     * @param title title of album
     * @return found album or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumAsync(title: String) = coroutineScope {
        async(Dispatchers.IO) { albumsDao.getAlbum(title) }
    }

    /**
     * Gets all albums with tracks
     * @return all albums and track relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumsWithTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { albumsAndTracksDao.getAlbumsWithTracks() }
    }

    /**
     * Gets album by it's track or null if there is no album with this track
     * @param trackAlbumId id of album
     * @return found album or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumOfTrackAsync(trackAlbumId: UUID) = coroutineScope {
        async(Dispatchers.IO) { albumsAndTracksDao.getAlbumByTrack(trackAlbumId) }
    }

    /**
     * Gets all albums of artist
     * @return list of all albums
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumsByArtistAsync(artistId: UUID) = coroutineScope {
        async(Dispatchers.IO) { artistsAndAlbumsDao.getAlbumsByArtist(artistId) }
    }

    /**
     * Gets all tracks
     * @return list with all tracks
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTracks() } }

    /**
     * Updates tracks
     * @param tracks [TrackOld] to change
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun updateTracks(vararg tracks: TrackOld) = updateEntitiesAsync(tracksDao, *tracks)

    /**
     * Adds tracks
     * @param tracks [TrackOld] to add
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun addTracks(vararg tracks: TrackOld) = addEntitiesAsync(tracksDao, *tracks)

    /**
     * Gets track by its id or null if it wasn't found
     * @param id id of track
     * @return found track or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTrackAsync(id: UUID) = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getTrack(id) }
    }

    /**
     * Gets track by its title or null if he wasn't found
     * @param title title of track
     * @return found title or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTrackAsync(title: String) = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getTrack(title) }
    }

    /**
     * Gets all tracks from album
     * @param albumId id of album
     * @return list with found tracks
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksFromAlbumAsync(albumId: UUID) = coroutineScope {
        async(Dispatchers.IO) { albumsAndTracksDao.getTracksFromAlbum(albumId) }
    }

    /**
     * Gets all track-artists relationships
     * @return list of all [TrackWithArtists] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksWithArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsAndTracksDao.getTracksWithArtists() }
    }

    /**
     * Gets all artist-tracks relationships with specified artist's id
     * @return list of all [ArtistWithTracks] relationships that matches [artistId]
     * @deprecated Now using android MediaStore instead of database
     */

    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksByArtistAsync(artistId: UUID) = coroutineScope {
        async(Dispatchers.IO) {
            artistsAndTracksDao
                .getArtistsWithTracks()
                .filter { it.artist.artistId == artistId }
        }
    }
}