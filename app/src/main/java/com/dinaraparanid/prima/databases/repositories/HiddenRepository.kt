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

    private suspend fun <T> updateEntityAsync(entity: T, dao: EntityDao<T>) = coroutineScope {
        launch(Dispatchers.IO) {
            dao.updateAsync(entity)
            refreshApplicationTracksAsync().join()
        }
    }

    /** Updates track asynchronously */
    suspend fun updateTrackAsync(track: HiddenTrack) =
        updateEntityAsync(track, tracksDao)

    /** Updates artist asynchronously */
    suspend fun updateArtistAsync(artist: HiddenArtist) =
        updateEntityAsync(artist, artistsDao)

    /** Updates playlist asynchronously */
    suspend fun updatePlaylistAsync(playlist: HiddenPlaylist.Entity) =
        updateEntityAsync(playlist, playlistsDao)

    private suspend fun <T> insertEntityAsync(entity: T, dao: EntityDao<T>) = coroutineScope {
        launch(Dispatchers.IO) {
            dao.insertAsync(entity)
            refreshApplicationTracksAsync().join()
        }
    }

    /** Adds new track to database asynchronously */
    suspend fun insertTrackAsync(track: HiddenTrack) =
        insertEntityAsync(track, tracksDao)

    /** Adds new artist to database asynchronously */
    suspend fun insertArtistAsync(artist: HiddenArtist) =
        insertEntityAsync(artist, artistsDao)

    /** Adds new playlist to database asynchronously */
    suspend fun insertPlaylistAsync(playlist: HiddenPlaylist) =
        insertEntityAsync(HiddenPlaylist.Entity(playlist), playlistsDao)

    private suspend fun <T> removeEntityAsync(entity: T, dao: EntityDao<T>) = coroutineScope {
        launch(Dispatchers.IO) {
            dao.removeAsync(entity)
            refreshApplicationTracksAsync().join()
        }
    }

    /** Removes track from database asynchronously */
    suspend fun removeTrackAsync(track: HiddenTrack) =
        removeEntityAsync(track, tracksDao)

    /** Removes artist from database asynchronously */
    suspend fun removeArtistAsync(artist: HiddenArtist) =
        removeEntityAsync(artist, artistsDao)

    /** Removes playlist from database asynchronously */
    suspend fun removePlaylistAsync(playlist: HiddenPlaylist.Entity) =
        removeEntityAsync(playlist, playlistsDao)

    /** Removes playlists by its title and type */
    suspend fun removePlaylistAsync(title: String, type: AbstractPlaylist.PlaylistType) =
        coroutineScope {
            launch(Dispatchers.IO) { playlistsDao.removePlaylistAsync(title, type.ordinal) }
        }
}