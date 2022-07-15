package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.ArtistWithTracks
import com.dinaraparanid.prima.databases.relationships.TrackWithArtists
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefDao

/**
 * [Dao] for [AlbumOld] and [TrackOld] relationships
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface ArtistsAndTracksDao : CrossRefDao<ArtistWithTracks> {

    /**
     * Gets all artist-tracks relationships
     * @return list of all [ArtistWithTracks] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM artist")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsWithTracks(): List<ArtistWithTracks>

    /**
     * Gets all track-artists relationships
     * @return list of all [TrackWithArtists] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM track")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksWithArtists(): List<TrackWithArtists>
}