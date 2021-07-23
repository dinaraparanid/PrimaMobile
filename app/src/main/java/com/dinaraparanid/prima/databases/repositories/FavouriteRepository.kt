package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.FavouriteDatabase
import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FavouriteRepository(context: Context) {
    companion object {
        private const val DATABASE_NAME = "favourite.db"

        private var INSTANCE: FavouriteRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = FavouriteRepository(context)
        }

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

    val tracksAsync: Deferred<List<FavouriteTrack>>
        get() = runBlocking { async { trackDao.getTracks() } }
    
    val artistsAsync: Deferred<List<FavouriteArtist>>
        get() = runBlocking { async { artistDao.getArtists() } }

    fun getTrackAsync(path: String): Deferred<FavouriteTrack?> = 
        runBlocking { async { trackDao.getTrack(path) } }
    
    fun getArtistAsync(name: String): Deferred<FavouriteArtist?> = 
        runBlocking { async { artistDao.getArtist(name) } }

    fun updateTrack(track: FavouriteTrack): Unit =
        runBlocking { launch { trackDao.updateTrack(track) } }

    fun updateArtist(artist: FavouriteArtist): Unit =
        runBlocking { launch { artistDao.updateArtist(artist) } }

    fun addTrack(track: FavouriteTrack): Unit =
        runBlocking { launch { trackDao.addTrack(track) } }

    fun addArtist(artist: FavouriteArtist): Unit =
        runBlocking { launch { artistDao.addArtist(artist) } }

    fun removeTrack(track: FavouriteTrack): Unit =
        runBlocking { launch { trackDao.removeTrack(track) } }

    fun removeArtist(artist: FavouriteArtist): Unit =
        runBlocking { launch { artistDao.removeArtist(artist) } }
}