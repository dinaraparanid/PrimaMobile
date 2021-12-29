package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.StatisticsDatabase
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsArtist
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsPlaylist
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import com.dinaraparanid.prima.databases.repositories.ImageRepository.Companion.initialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Repository for statistics */

class StatisticsRepository private constructor(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "statistics.db"
        private var INSTANCE: StatisticsRepository? = null
        private val mutex = Mutex()

        /** Initialises repository only once */

        @JvmStatic
        internal fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = StatisticsRepository(context)
        }

        /**
         * Gets repository's instance with mutex protection
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal val instance
            @JvmStatic
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("Statisticsrepositore isn't initialized")

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

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            StatisticsDatabase::class.java,
            DATABASE_NAME
        )
        .addMigrations()
        .build()

    private val trackDao = database.statisticsTracksDao()
    private val artistDao = database.statisticsArtistDao()
    private val playlistDao = database.statisticsPlaylistDao()

    /**
     * Gets all statistics tracks asynchronously
     * @return all statistics tracks
     */

    suspend fun getTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { trackDao.getTracksAsync() }
    }

    /**
     * Gets all statistics artists asynchronously
     * @return all statistics artists
     */

    suspend fun getArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtistsAsync() }
    }

    /**
     * Gets all statistics playlists asynchronously
     * @return all statistics playlists
     */

    suspend fun getPlaylistsAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistDao.getPlaylistsAsync() }
    }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    suspend fun getTrackAsync(path: String) = coroutineScope {
        async(Dispatchers.IO) { trackDao.getTrackAsync(path) }
    }

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    suspend fun getArtistAsync(name: String) = coroutineScope {
        async(Dispatchers.IO) { artistDao.getArtistAsync(name) }
    }

    /**
     * Gets playlist by its title and type asynchronously
     * @param title Playlist's title
     * @param type [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType] as [Int]
     * @return playlist or null if it doesn't exist
     */

    suspend fun getPlaylistAsync(title: String, type: Int) = coroutineScope {
        async(Dispatchers.IO) { playlistDao.getPlaylistAsync(title, type) }
    }

    /**
     * Updates track's title, artist, album and count by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     * @param count new count
     */

    suspend fun updateTrackAsync(
        path: String,
        title: String,
        artist: String,
        album: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            trackDao.updateTrackAsync(
                path,
                title,
                artist,
                album,
                count,
                countDaily,
                countWeekly,
                countMonthly,
                countYearly
            )
        }
    }

    /**
     * Updates playlist's title and count by its id
     * @param id playlist's id
     * @param title new title
     */

    suspend fun updatePlaylistAsync(
        id: Long,
        title: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            playlistDao.updatePlaylistAsync(
                id,
                title,
                count,
                countDaily,
                countWeekly,
                countMonthly,
                countYearly
            )
        }
    }

    /** Adds tracks asynchronously */

    suspend fun addTrackAsync(track: StatisticsTrack) = coroutineScope {
        launch(Dispatchers.IO) { trackDao.insertAsync(track) }
    }

    /** Adds new artist asynchronously */

    suspend fun addArtistAsync(artist: StatisticsArtist) = coroutineScope {
        launch(Dispatchers.IO) { artistDao.insertAsync(artist) }
    }

    /** Adds new playlist asynchronously */

    suspend fun addPlaylistAsync(playlist: StatisticsPlaylist.Entity) = coroutineScope {
        launch(Dispatchers.IO) { playlistDao.insertAsync(playlist) }
    }

    /** Removes track asynchronously */

    suspend fun removeTrackAsync(track: StatisticsTrack) = coroutineScope {
        launch(Dispatchers.IO) { trackDao.removeAsync(track) }
    }

    /** Removes artist asynchronously */

    suspend fun removeArtistAsync(artist: StatisticsArtist) = coroutineScope {
        launch(Dispatchers.IO) { artistDao.removeAsync(artist) }
    }

    /** Removes playlist asynchronously */

    suspend fun removePlaylistAsync(playlist: StatisticsPlaylist.Entity) = coroutineScope {
        launch(Dispatchers.IO) { playlistDao.removeAsync(playlist) }
    }
}