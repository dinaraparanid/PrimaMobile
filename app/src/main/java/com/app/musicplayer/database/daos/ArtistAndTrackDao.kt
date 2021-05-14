package com.app.musicplayer.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.app.musicplayer.database.relationships.ArtistWithTracks
import com.app.musicplayer.database.relationships.TrackWithArtists

@Dao
interface ArtistAndTrackDao {
    @Transaction
    @Query("SELECT * FROM track")
    fun getTracksWithArtists(): LiveData<List<TrackWithArtists>>

    @Transaction
    @Query("SELECT * FROM artist")
    fun getArtistsWithTracks(): LiveData<List<ArtistWithTracks>>
}