package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.database.relationships.ArtistWithTracks
import com.dinaraparanid.prima.database.relationships.TrackWithArtists

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistAndTrackDao {
    @Transaction
    @Query("SELECT * FROM track")
    fun getTracksWithArtists(): LiveData<List<TrackWithArtists>>

    @Transaction
    @Query("SELECT * FROM artist")
    fun getArtistsWithTracks(): LiveData<List<ArtistWithTracks>>
}