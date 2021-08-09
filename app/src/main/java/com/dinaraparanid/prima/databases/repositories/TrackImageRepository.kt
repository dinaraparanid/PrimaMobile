package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.TrackImagesDatabase
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository.Companion.initialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TrackImageRepository(context: Context) {
    internal companion object {
        private const val DATABASE_NAME = "track_images.db"
        private var INSTANCE: TrackImageRepository? = null

        /**
         * Initialises repository only once
         */

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = TrackImageRepository(context)
        }

        /**
         * Gets repository's instance
         * @throws UninitializedPropertyAccessException
         * if repository wasn't initialized
         * @return repository's instance
         * @see initialize
         */

        val instance: TrackImageRepository
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("TrackImageRepository is not initialized")
    }

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            TrackImagesDatabase::class.java,
            DATABASE_NAME
        )
        .build()

    private val trackImageDao = database.trackImageDao()

    /**
     * Gets all tracks with their images asynchronously
     * @return all tracks with their images
     */

    suspend fun getTracksWithImagesAsync() = coroutineScope {
        async(Dispatchers.IO) { trackImageDao.getTracksWithImages() }
    }

    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    suspend fun getTrackWithImageAsync(path: String) = coroutineScope {
        async(Dispatchers.IO) { trackImageDao.getTrackWithImage(path) }
    }

    /** Updates track with its image asynchronously */

    suspend fun updateTrackWithImageAsync(track: TrackImage) = coroutineScope {
        async(Dispatchers.IO) { trackImageDao.updateTrackWithImageAsync(track) }
    }

    /** Adds tracks with their images asynchronously */

    suspend fun addTrackWithImageAsync(track: TrackImage) = coroutineScope {
        async(Dispatchers.IO) { trackImageDao.addTrackWithImageAsync(track) }
    }

    suspend fun removeTrackWithImageAsync(track: TrackImage) = coroutineScope {
        async(Dispatchers.IO) { trackImageDao.removeTrackWithImageAsync(track) }
    }
}