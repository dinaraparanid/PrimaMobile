package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.FavouriteDatabase
import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.*

/**
 * Repository for user's favourite tracks and artists
 */

class FavouriteRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "favourite.db"

        private var INSTANCE: FavouriteRepository? = null

        /**
         * Initialises repository only once
         */

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = FavouriteRepository(context)
        }

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        val instance: FavouriteRepository
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("FavouriteRepository is not initialized")
    }

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            FavouriteDatabase::class.java,
            DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()

    private val trackDao = database.trackDao()
    private val artistDao = database.artistDao()

    /**
     * Gets all favourite tracks asynchronously
     * @return all favourite tracks
     */

    suspend fun getTracksAsync(): Deferred<List<FavouriteTrack>> =
        coroutineScope { async(Dispatchers.IO) { trackDao.getTracksAsync() } }

    /**
     * Gets all favourite artists asynchronously
     * @return all favourite artists
     */

    suspend fun getArtistsAsync(): Deferred<List<FavouriteArtist>> =
        coroutineScope { async(Dispatchers.IO) { artistDao.getArtistsAsync() } }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    suspend fun getTrackAsync(path: String): Deferred<FavouriteTrack?> =
        coroutineScope { async(Dispatchers.IO) { trackDao.getTrackAsync(path) } }

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    suspend fun getArtistAsync(name: String): Deferred<FavouriteArtist?> =
        coroutineScope { async(Dispatchers.IO) { artistDao.getArtistAsync(name) } }

    /** Updates track asynchronously */

    suspend fun updateTrackAsync(track: FavouriteTrack): Unit =
        coroutineScope { launch(Dispatchers.IO) { trackDao.updateTrackAsync(track) } }

    /** Updates artist */

    suspend fun updateArtistAsync(artist: FavouriteArtist): Unit =
        coroutineScope { launch(Dispatchers.IO) { artistDao.updateArtistAsync(artist) } }

    /** Adds tracks asynchronously */

    suspend fun addTrackAsync(track: FavouriteTrack): Unit =
        coroutineScope { launch(Dispatchers.IO) { trackDao.addTrackAsync(track) } }

    /** Adds new artist asynchronously */

    suspend fun addArtistAsync(artist: FavouriteArtist): Unit =
        coroutineScope { launch(Dispatchers.IO) { artistDao.addArtistAsync(artist) } }

    /** Removes track asynchronously */

    suspend fun removeTrackAsync(track: FavouriteTrack): Unit =
        coroutineScope { launch(Dispatchers.IO) { trackDao.removeTrackAsync(track) } }

    /** Removes artist asynchronously */

    suspend fun removeArtistAsync(artist: FavouriteArtist): Unit =
        coroutineScope { launch(Dispatchers.IO) { artistDao.removeArtistAsync(artist) } }
}