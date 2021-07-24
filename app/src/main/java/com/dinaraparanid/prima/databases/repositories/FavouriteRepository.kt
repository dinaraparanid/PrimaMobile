package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.FavouriteDatabase
import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Repository for user's favourite tracks and artists
 */

class FavouriteRepository(context: Context) {
    companion object {
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

    val tracksAsync: Deferred<List<FavouriteTrack>>
        get() = runBlocking { async { trackDao.getTracksAsync() } }

    /**
     * Gets all favourite artists asynchronously
     * @return all favourite artists
     */

    val artistsAsync: Deferred<List<FavouriteArtist>>
        get() = runBlocking { async { artistDao.getArtistsAsync() } }

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    fun getTrackAsync(path: String): Deferred<FavouriteTrack?> = 
        runBlocking { async { trackDao.getTrackAsync(path) } }

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    fun getArtistAsync(name: String): Deferred<FavouriteArtist?> = 
        runBlocking { async { artistDao.getArtistAsync(name) } }

    /** Updates track asynchronously */

    fun updateTrack(track: FavouriteTrack): Unit =
        runBlocking { launch { trackDao.updateTrackAsync(track) } }

    /** Updates artist */

    fun updateArtist(artist: FavouriteArtist): Unit =
        runBlocking { launch { artistDao.updateArtistAsync(artist) } }

    /** Adds tracks asynchronously */

    fun addTrack(track: FavouriteTrack): Unit =
        runBlocking { launch { trackDao.addTrackAsync(track) } }

    /** Adds new artist asynchronously */

    fun addArtist(artist: FavouriteArtist): Unit =
        runBlocking { launch { artistDao.addArtistAsync(artist) } }

    /** Removes track asynchronously */

    fun removeTrack(track: FavouriteTrack): Unit =
        runBlocking { launch { trackDao.removeTrackAsync(track) } }

    /** Removes artist asynchronously */

    fun removeArtist(artist: FavouriteArtist): Unit =
        runBlocking { launch { artistDao.removeArtistAsync(artist) } }
}