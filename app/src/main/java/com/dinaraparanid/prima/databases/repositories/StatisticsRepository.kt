package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.StatisticsDatabase
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsArtist
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsPlaylist
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import com.dinaraparanid.prima.databases.repositories.CoversRepository.Companion.initialize
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Repository for statistics */

class StatisticsRepository private constructor(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "statistics.db"

        @JvmStatic
        private var INSTANCE: StatisticsRepository? = null

        @JvmStatic
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [StatisticsRepository] is already initialized
         */

        @JvmStatic
        internal fun initialize(context: Context) {
            if (INSTANCE != null) throw IllegalStateException("StatisticsRepository is already initialized")
            INSTANCE = StatisticsRepository(context)
        }

        /**
         * Gets repository's instance without any synchronization
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        internal val instance
            @JvmStatic
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("StatisticsRepository isn't initialized")

        /**
         * Gets repository's instance
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
        .fallbackToDestructiveMigration()
        .build()

    private val tracksDao = database.statisticsTracksDao()
    private val artistsDao = database.statisticsArtistsDao()
    private val playlistsDao = database.statisticsPlaylistDao()

    /**
     * Gets full statistics for track, artist and playlist
     * and returns them as [Triple] of [Deferred]
     */

    internal suspend inline fun getFullFragmentStatistics(
        actions: StatisticsRepository.() -> Triple<Deferred<StatisticsTrack?>, Deferred<StatisticsArtist?>, Deferred<StatisticsPlaylist.Entity?>>
    ) = mutex.withLock { actions(this) }

    /**
     * Gets all statistics tracks asynchronously
     * @return all statistics tracks
     */

    suspend fun getTracksAsync() = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getTracksAsync() }
    }

    /**
     * Gets all statistics artists asynchronously
     * @return all statistics artists
     */

    suspend fun getArtistsAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getArtistsAsync() }
    }

    /**
     * Gets all statistics playlists asynchronously
     * @return all statistics playlists
     */

    suspend fun getPlaylistsAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getPlaylistsAsync() }
    }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    suspend fun getTrackAsync(path: String) = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getTrackAsync(path) }
    }

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    suspend fun getArtistAsync(name: String) = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getArtistAsync(name) }
    }

    /**
     * Gets playlist by its title and type asynchronously
     * @param title Playlist's title
     * @param type [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType] as [Int]
     * @return playlist or null if it doesn't exist
     */

    suspend fun getPlaylistAsync(title: String, type: Int) = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getPlaylistAsync(title, type) }
    }

    /**
     * Updates track's title, artist, album and count by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     * @param numberInAlbum track's position in album or -1 if no info
     */

    suspend fun updateTrackAsync(
        path: String,
        title: String,
        artist: String,
        album: String,
        numberInAlbum: Byte,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            tracksDao.updateTrackAsync(
                path, title, artist, album, numberInAlbum,
                count, countDaily, countWeekly, countMonthly, countYearly
            )
        }
    }

    /**
     * Updates track's title, artist, album by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     * @param numberInAlbum track's position in album or -1 if no info
     */

    suspend fun updateTrackAsync(
        path: String,
        title: String,
        artist: String,
        album: String,
        numberInAlbum: Byte
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            tracksDao.updateTrackAsync(path, title, artist, album, numberInAlbum)
        }
    }

    /**
     * Updates artist's count by its path
     * @param name artist's name
     */

    suspend fun updateArtistAsync(
        name: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            artistsDao.updateArtistAsync(
                name, count, countDaily, countWeekly, countMonthly, countYearly
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
            playlistsDao.updatePlaylistAsync(
                id, title, count, countDaily, countWeekly, countMonthly, countYearly
            )
        }
    }

    private suspend fun <T> addEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope { launch(Dispatchers.IO) { dao.insertAsync(*entities) } }

    private suspend fun <T> removeEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope { launch(Dispatchers.IO) { dao.removeAsync(*entities) } }

    private suspend fun <T> updateEntitiesAsync(dao: EntityDao<T>, vararg entities: T) =
        coroutineScope { launch(Dispatchers.IO) { dao.updateAsync(*entities) } }

    /** Adds tracks asynchronously */
    suspend fun addTracksAsync(vararg tracks: StatisticsTrack) =
        addEntitiesAsync(tracksDao, *tracks)

    /** Adds new artists asynchronously */
    suspend fun addArtistsAsync(vararg artists: StatisticsArtist) =
        addEntitiesAsync(artistsDao, *artists)

    /** Adds new playlists asynchronously */
    suspend fun addPlaylistsAsync(vararg playlists: StatisticsPlaylist.Entity) =
        addEntitiesAsync(playlistsDao, *playlists)

    /** Removes tracks asynchronously */
    suspend fun removeTracksAsync(vararg tracks: StatisticsTrack) =
        removeEntitiesAsync(tracksDao, *tracks)

    /** Removes track by its path asynchronously */
    suspend fun removeTrackAsync(path: String) = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.removeTrackAsync(path) }
    }

    /** Removes artists asynchronously */
    suspend fun removeArtistsAsync(vararg artists: StatisticsArtist) =
        removeEntitiesAsync(artistsDao, *artists)

    /** Removes playlists asynchronously */
    suspend fun removePlaylistsAsync(vararg playlists: StatisticsPlaylist.Entity) =
        removeEntitiesAsync(playlistsDao, *playlists)

    /** Removes custom playlist by its title asynchronously */
    suspend fun removeCustomPlaylistAsync(title: String) = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.removeCustomPlaylistAsync(title) }
    }

    /** Clears all counting statistics for all tracks */
    suspend fun clearAllTracksStatisticsAsync() = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.clearAllTracksStatisticsAsync() }
    }

    /** Clears all counting statistics for all artists */
    suspend fun clearAllArtistsStatisticsAsync() = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.clearAllArtistsStatisticsAsync() }
    }

    /** Clears all counting statistics for all playlists */
    suspend fun clearAllPlaylistsStatisticsAsync() = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.clearAllPlaylistsStatisticsAsync() }
    }

    /** Refreshes daily statistics for all tracks */
    suspend fun refreshTracksCountingDailyAsync() = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.refreshTracksCountingDailyAsync() }
    }

    /** Refreshes daily statistics for all artists */
    suspend fun refreshArtistsCountingDailyAsync() = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.refreshArtistsCountingDailyAsync() }
    }

    /** Refreshes daily statistics for all playlists */
    suspend fun refreshPlaylistsCountingDailyAsync() = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.refreshPlaylistsCountingDailyAsync() }
    }

    /** Refreshes weekly statistics for all tracks */
    suspend fun refreshTracksCountingWeeklyAsync() = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.refreshTracksCountingWeeklyAsync() }
    }

    /** Refreshes weekly statistics for all artists */
    suspend fun refreshArtistsCountingWeeklyAsync() = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.refreshArtistsCountingWeeklyAsync() }
    }

    /** Refreshes weekly statistics for all playlists */
    suspend fun refreshPlaylistsCountingWeeklyAsync() = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.refreshPlaylistsCountingWeeklyAsync() }
    }

    /** Refreshes monthly statistics for all tracks */
    suspend fun refreshTracksCountingMonthlyAsync() = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.refreshTracksCountingMonthlyAsync() }
    }

    /** Refreshes monthly statistics for all artists */
    suspend fun refreshArtistsCountingMonthlyAsync() = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.refreshArtistsCountingMonthlyAsync() }
    }

    /** Refreshes monthly statistics for all playlists */
    suspend fun refreshPlaylistsCountingMonthlyAsync() = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.refreshPlaylistsCountingMonthlyAsync() }
    }

    /** Refreshes yearly statistics for all tracks */
    suspend fun refreshTracksCountingYearlyAsync() = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.refreshTracksCountingYearlyAsync() }
    }

    /** Refreshes yearly statistics for all artists */
    suspend fun refreshArtistsCountingYearlyAsync() = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.refreshArtistsCountingYearlyAsync() }
    }

    /** Refreshes yearly statistics for all playlists */
    suspend fun refreshPlaylistsCountingYearlyAsync() = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.refreshPlaylistsCountingYearlyAsync() }
    }

    /**
     * Increments counting statistics for a certain track
     * @param path track's path which statistics should be updated
     */

    suspend fun incrementTrackCountingAsync(path: String) = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.incrementTrackCountingAsync(path) }
    }

    /**
     * Increments counting statistics for a certain artist
     * @param name artist's name which statistics should be updated
     */

    suspend fun incrementArtistCountingAsync(name: String) = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.incrementArtistCountingAsync(name) }
    }

    /**
     * Increments counting statistics for a certain playlist
     * @param title playlist's title
     * @param type playlist's type ordinal of [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType]
     */

    suspend fun incrementPlaylistCountingAsync(title: String, type: Int) = coroutineScope {
        launch { playlistsDao.incrementPlaylistCountingAsync(title, type) }
    }

    /** Gets track with the largest count param */
    suspend fun getMaxCountingTrackAsync() = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getMaxCountingTrack() }
    }

    /** Gets artist with the largest count param */
    suspend fun getMaxCountingArtistAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getMaxCountingArtist() }
    }

    /** Gets playlist with the largest count param */
    suspend fun getMaxCountingPlaylistAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getMaxCountingPlaylist() }
    }

    /** Gets track with the largest daily count param */
    suspend fun getMaxCountingTrackDailyAsync() = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getMaxCountingTrackDaily() }
    }

    /** Gets artist with the largest daily count param */
    suspend fun getMaxCountingArtistDailyAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getMaxCountingArtistDaily() }
    }

    /** Gets playlist with the largest daily count param */
    suspend fun getMaxCountingPlaylistDailyAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getMaxCountingPlaylistDaily() }
    }

    /** Gets track with the largest weekly count param */
    suspend fun getMaxCountingTrackWeeklyAsync() = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getMaxCountingTrackWeekly() }
    }

    /** Gets artist with the largest weekly count param */
    suspend fun getMaxCountingArtistWeeklyAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getMaxCountingArtistWeekly() }
    }

    /** Gets playlist with the largest weekly count param */
    suspend fun getMaxCountingPlaylistWeeklyAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getMaxCountingPlaylistWeekly() }
    }

    /** Gets track with the largest monthly count param */
    suspend fun getMaxCountingTrackMonthlyAsync() = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getMaxCountingTrackMonthly() }
    }

    /** Gets artist with the largest monthly count param */
    suspend fun getMaxCountingArtistMonthlyAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getMaxCountingArtistMonthly() }
    }

    /** Gets playlist with the largest monthly count param */
    suspend fun getMaxCountingPlaylistMonthlyAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getMaxCountingPlaylistMonthly() }
    }

    /** Gets track with the largest yearly count param */
    suspend fun getMaxCountingTrackYearlyAsync() = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getMaxCountingTrackYearly() }
    }

    /** Gets artist with the largest yearly count param */
    suspend fun getMaxCountingArtistYearlyAsync() = coroutineScope {
        async(Dispatchers.IO) { artistsDao.getMaxCountingArtistYearly() }
    }

    /** Gets playlist with the largest yearly count param */
    suspend fun getMaxCountingPlaylistYearlyAsync() = coroutineScope {
        async(Dispatchers.IO) { playlistsDao.getMaxCountingPlaylistYearly() }
    }

    /** Gets most listened track from the artist or null if there are no such tracks */
    suspend fun getMostListenedTrackByArtistAsync(artist: String) = coroutineScope {
        async(Dispatchers.IO) { tracksDao.getMostListenedTrackByArtistAsync(artist) }
    }

    /** Clears the whole tracks' table */
    private suspend fun clearTracksTableAsync() = coroutineScope {
        launch(Dispatchers.IO) { tracksDao.clearTable() }
    }

    /** Clears the whole artists' table */
    private suspend fun clearArtistsTableAsync() = coroutineScope {
        launch(Dispatchers.IO) { artistsDao.clearTable() }
    }

    /** Clears the whole playlists' table */
    private suspend fun clearPlaylistTableAsync() = coroutineScope {
        launch(Dispatchers.IO) { playlistsDao.clearTable() }
    }

    /** Clears all statistics tables */
    suspend fun clearAllStatisticsAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            clearTracksTableAsync()
            clearArtistsTableAsync()
            clearPlaylistTableAsync()
        }
    }
}