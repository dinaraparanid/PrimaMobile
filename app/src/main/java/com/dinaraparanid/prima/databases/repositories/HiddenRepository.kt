package com.dinaraparanid.prima.databases.repositories

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.databases.databases.HiddenDatabase
import com.dinaraparanid.prima.databases.entities.hidden.HiddenArtist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

/** Repository for hidden content */

class HiddenRepository private constructor(private val application: WeakReference<MainApplication>) {
    internal companion object {
        private const val DATABASE_NAME = "hidden_tracks.db"

        @JvmStatic
        private var INSTANCE: HiddenRepository? = null

        @JvmStatic
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [HiddenRepository] is already initialized
         */

        @JvmStatic
        internal fun initialize(application: MainApplication) {
            if (INSTANCE != null) throw IllegalStateException("HiddenRepository is already initialized")
            INSTANCE = HiddenRepository((application))
        }

        /**
         * Gets repository's instance without any synchronization
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        private inline val instance
            @JvmStatic
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("HiddenRepository is not initialized")

        /**
         * Gets repository's instance with mutex's protection
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        @JvmStatic
        internal suspend fun getInstanceSynchronized() = mutex.withLock { instance }
    }

    constructor(application: MainApplication) : this(WeakReference(application))

    private val database = Room
        .databaseBuilder(
            application.unchecked.applicationContext,
            HiddenDatabase::class.java,
            DATABASE_NAME
        )
        .addMigrations(
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE HiddenArtists (name TEXT NOT NULL, PRIMARY KEY (name))")
                    database.execSQL("CREATE TABLE HiddenPlaylists (id INTEGER NOT NULL, title TEXT NOT NULL, type INTEGER NOT NULL, PRIMARY KEY (id))")
                }
            }
        )
        .build()

    private val tracksDao = database.hiddenTracksDao()
    private val artistsDao = database.hiddenArtistsDao()
    private val playlistsDao = database.hiddenPlaylistsDao()

    /** Gets all hidden tracks from database */

    suspend fun getTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTracksAsync() } }

    /**
     * Gets all hidden tracks of artist asynchronously
     * @return all hidden tracks of artist
     */

    suspend fun getTracksOfArtistAsync(artist: String) =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTracksOfArtistAsync(artist) } }

    /**
     * Gets all hidden tracks of album asynchronously
     * @return all hidden tracks of album
     */

    suspend fun getTracksOfAlbumAsync(album: String) =
        coroutineScope { async(Dispatchers.IO) { tracksDao.getTracksOfAlbumAsync(album) } }

    /** Gets all hidden artists from database */

    suspend fun getArtistsAsync() =
        coroutineScope { async(Dispatchers.IO) { artistsDao.getArtistsAsync() } }

    /** Gets all hidden artists from database */

    suspend fun getPlaylistsAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getPlaylistsAsync().map(::HiddenPlaylist) }
    }

    /** Gets all hidden albums */

    suspend fun getAlbumsAsync() = coroutineScope {
        async(Dispatchers.IO) {
            playlistsDao.getPlaylistsByTypeAsync(type = AbstractPlaylist.PlaylistType.ALBUM.ordinal)
        }
    }

    /** Gets all hidden albums */

    suspend fun getCustomPlaylistAsync() = coroutineScope {
        async(Dispatchers.IO) {
            playlistsDao.getPlaylistsByTypeAsync(type = AbstractPlaylist.PlaylistType.CUSTOM.ordinal)
        }
    }

    /**
     * Gets playlists by its title and type
     * @return found playlist or null if it doesn't exist
     */

    suspend fun getPlaylistAsync(title: String, type: AbstractPlaylist.PlaylistType) =
        coroutineScope {
            async(Dispatchers.IO) { playlistsDao.getPlaylistAsync(title, type.ordinal) }
        }

    private suspend fun refreshApplicationTracksAsync() =
        (application.unchecked as Loader<*>).loadAsync()

    private suspend fun <T> updateEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope {
            launch(Dispatchers.IO) {
                dao.updateAsync(*entities)
                refreshApplicationTracksAsync().join()
            }
        }

    /** Updates tracks asynchronously */
    suspend fun updateTracksAsync(vararg tracks: HiddenTrack) =
        updateEntitiesAsync(tracksDao, *tracks)

    /** Updates artists asynchronously */
    suspend fun updateArtistsAsync(vararg artists: HiddenArtist) =
        updateEntitiesAsync(artistsDao, *artists)

    /** Updates playlists asynchronously */
    suspend fun updatePlaylistsAsync(vararg playlists: HiddenPlaylist.Entity) =
        updateEntitiesAsync(playlistsDao, *playlists)

    private suspend fun <T> insertEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope {
            launch(Dispatchers.IO) {
                dao.insertAsync(*entities)
                refreshApplicationTracksAsync().join()
            }
        }

    /** Adds new tracks to database asynchronously */
    suspend fun insertTracksAsync(vararg tracks: HiddenTrack) =
        insertEntitiesAsync(tracksDao, *tracks)

    /** Adds new artists to database asynchronously */
    suspend fun insertArtistsAsync(vararg artists: HiddenArtist) =
        insertEntitiesAsync(artistsDao, *artists)

    /** Adds new playlists to database asynchronously */
    suspend fun insertPlaylistsAsync(vararg playlists: HiddenPlaylist) = insertEntitiesAsync(
        playlistsDao,
        *playlists.map { HiddenPlaylist.Entity(it) }.toTypedArray()
    )

    private suspend fun <T> removeEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope {
            launch(Dispatchers.IO) {
                dao.removeAsync(*entities)
                refreshApplicationTracksAsync().join()
            }
        }

    /** Removes tracks from database asynchronously */
    suspend fun removeTracksAsync(vararg tracks: HiddenTrack) =
        removeEntitiesAsync(tracksDao, *tracks)

    /** Removes all hidden tracks of artist asynchronously */
    suspend fun removeTracksOfArtistAsync(artist: String) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeTracksOfArtistAsync(artist) } }

    /** Removes all hidden tracks of album asynchronously */
    suspend fun removeTracksOfAlbumAsync(album: String) =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeTracksOfAlbumAsync(album) } }

    /** Removes all hidden tracks from table */
    suspend fun removeAllTracksAsync() =
        coroutineScope { launch(Dispatchers.IO) { tracksDao.removeAllTracksAsync() } }

    /** Removes artists from database asynchronously */
    suspend fun removeArtistsAsync(vararg artists: HiddenArtist) =
        removeEntitiesAsync(artistsDao, *artists)

    /** Removes playlists from database asynchronously */
    suspend fun removePlaylistsAsync(vararg playlists: HiddenPlaylist.Entity) =
        removeEntitiesAsync(playlistsDao, *playlists)

    /** Removes playlists by its title and type */
    suspend fun removePlaylistAsync(title: String, type: AbstractPlaylist.PlaylistType) =
        coroutineScope {
            launch(Dispatchers.IO) { playlistsDao.removePlaylistAsync(title, type.ordinal) }
        }
}