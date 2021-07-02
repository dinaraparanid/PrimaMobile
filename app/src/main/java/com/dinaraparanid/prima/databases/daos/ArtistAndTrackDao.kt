package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.relationships.ArtistWithTracks
import com.dinaraparanid.prima.databases.relationships.TrackWithArtists

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistAndTrackDao {
    @Transaction
    @Query("SELECT * FROM track")
    suspend fun getTracksWithArtists(): List<TrackWithArtists>

    @Transaction
    @Query("SELECT * FROM artist")
    suspend fun getArtistsWithTracks(): List<ArtistWithTracks>
}