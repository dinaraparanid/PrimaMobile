package com.dinaraparanid.prima.databases.repositories

import android.content.Context
import androidx.room.Room
import com.dinaraparanid.prima.databases.databases.FavouriteDatabase
import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
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
                ?: throw IllegalStateException("FavouriteRepository is not initialized")
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

    val tracks: List<FavouriteTrack> get() = runBlocking { trackDao.getTracks() }
    val artists: List<FavouriteArtist> get() = runBlocking { artistDao.getArtists() }

    fun getTrack(path: String): FavouriteTrack? = runBlocking { trackDao.getTrack(path) }
    fun getArtist(name: String): FavouriteArtist? = runBlocking { artistDao.getArtist(name) }

    fun updateTrack(track: FavouriteTrack): Unit = runBlocking { trackDao.updateTrack(track) }
    fun updateArtist(artist: FavouriteArtist): Unit = runBlocking { artistDao.updateArtist(artist) }

    fun addTrack(track: FavouriteTrack): Unit = runBlocking { trackDao.addTrack(track) }
    fun addArtist(artist: FavouriteArtist): Unit = runBlocking { artistDao.addArtist(artist) }

    fun removeTrack(track: FavouriteTrack): Unit = runBlocking { trackDao.removeTrack(track) }
    fun removeArtist(artist: FavouriteArtist): Unit = runBlocking { artistDao.removeArtist(artist) }
}