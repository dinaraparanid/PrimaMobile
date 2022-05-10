package com.dinaraparanid.prima.databases.repositories

import androidx.room.Room
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.databases.databases.HiddenTracksDatabase
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.Loader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

/** Repository for hidden tracks */

class HiddenTracksRepository private constructor(private val application: WeakReference<MainApplication>) {
    internal companion object {
        private const val DATABASE_NAME = "hidden_tracks.db"

        @JvmStatic
        private var INSTANCE: HiddenTracksRepository? = null

        @JvmStatic
        private val mutex = Mutex()

        /**
         * Initialises repository only once
         * @throws IllegalStateException if [HiddenTracksRepository] is already initialized
         */

        @JvmStatic
        internal fun initialize(application: MainApplication) {
            if (INSTANCE != null) throw IllegalStateException("HiddenTracksRepository is already initialized")
            INSTANCE = HiddenTracksRepository((application))
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
                ?: throw UninitializedPropertyAccessException("HiddenTracksRepository is not initialized")

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
            HiddenTracksDatabase::class.java,
            DATABASE_NAME
        )
        .build()

    private val trackDao = database.hiddenTracksDao()

    /** Gets all hidden tracks from database */

    suspend fun getTracksAsync() =
        coroutineScope { async(Dispatchers.IO) { trackDao.getTracksAsync() } }

    private suspend fun refreshApplicationTracksAsync() =
        (application.unchecked as Loader<*>).loadAsync()

    /** Updates track asynchronously */

    suspend fun updateTrackAsync(track: HiddenTrack) = coroutineScope {
        launch(Dispatchers.IO) {
            trackDao.updateAsync(track)
            refreshApplicationTracksAsync().join()
        }
    }

    /** Adds new track to database asynchronously */

    suspend fun insertTrackAsync(track: HiddenTrack) = coroutineScope {
        launch(Dispatchers.IO) {
            trackDao.insertAsync(track)
            refreshApplicationTracksAsync().join()
        }
    }

    /** Removes track from database asynchronously */

    suspend fun removeTrackAsync(track: HiddenTrack) = coroutineScope {
        launch(Dispatchers.IO) {
            trackDao.removeAsync(track)
            refreshApplicationTracksAsync().join()
        }
    }
}