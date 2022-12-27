package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.ArtistOldWithTracksOld
import com.dinaraparanid.prima.databases.relationships.TrackOldWithArtistsOld
import com.dinaraparanid.prima.databases.daos.CrossRefDao
import java.util.*

/**
 * [Dao] for [AlbumOld] and [TrackOld] relationships
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface ArtistsAndTracksDao : CrossRefDao<ArtistOldWithTracksOld> {

    /**
     * Gets all artist-tracks relationships
     * @return list of all [ArtistOldWithTracksOld] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM artist")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsWithTracks(): List<ArtistOldWithTracksOld>

    /**
     * Gets all track-artists relationships
     * @return list of all [TrackOldWithArtistsOld] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM track")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksWithArtists(): List<TrackOldWithArtistsOld>

    /**
     * Gets all artists by track
     * @return list of all [ArtistOldWithTracksOld] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM artist_track WHERE track_id = :trackId")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsByTrackAsync(trackId: UUID): List<ArtistOldWithTracksOld>

    /**
     * Gets all artist-tracks relationships with specified artist's id
     * @return list of all [ArtistOldWithTracksOld] relationships that matches [artistId]
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM artist_track WHERE track_id = :artistId")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksByArtistAsync(artistId: UUID): List<ArtistOldWithTracksOld>
}