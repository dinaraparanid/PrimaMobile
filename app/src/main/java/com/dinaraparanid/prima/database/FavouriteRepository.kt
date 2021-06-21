package com.dinaraparanid.prima.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.dinaraparanid.prima.core.FavouriteArtist
import com.dinaraparanid.prima.core.FavouriteTrack
import java.util.concurrent.Executors

class FavouriteRepository(context: Context) {
    companion object {
        private const val DATABASE_NAME = "favourite.db"

        private var INSTANCE: FavouriteRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = FavouriteRepository(context)
        }

        fun getInstance(): FavouriteRepository =
            INSTANCE ?: throw IllegalStateException("FavouriteRepository is not initialized")
    }

    private val database = Room
        .databaseBuilder(
            context.applicationContext,
            FavouriteDatabase::class.java,
            DATABASE_NAME
        )
        .allowMainThreadQueries() // it's a small database and I need to get info now
        .build()

    private val trackDao = database.trackDao()
    private val artistDao = database.artistDao()
    private val executor = Executors.newSingleThreadExecutor()

    val tracks: List<FavouriteTrack> get() = trackDao.getTracks()
    val artists: List<FavouriteArtist> get() = artistDao.getArtists()

    fun getTrack(path: String): FavouriteTrack? = trackDao.getTrack(path)
    fun getArtist(name: String): FavouriteArtist? = artistDao.getArtist(name)

    fun updateTrack(track: FavouriteTrack): Unit = executor.execute { trackDao.updateTrack(track) }
    fun updateArtist(artist: FavouriteArtist): Unit =
        executor.execute { artistDao.updateArtist(artist) }

    fun addTrack(track: FavouriteTrack): Unit = executor.execute { trackDao.addTrack(track) }
    fun addArtist(artist: FavouriteArtist): Unit = executor.execute { artistDao.addArtist(artist) }

    fun removeTrack(track: FavouriteTrack): Unit = executor.execute { trackDao.removeTrack(track) }
    fun removeArtist(artist: FavouriteArtist): Unit =
        executor.execute { artistDao.removeArtist(artist) }
}