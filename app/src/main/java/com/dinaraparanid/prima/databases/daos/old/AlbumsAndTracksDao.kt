package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.AlbumOldAndTrackOld
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefDao
import java.util.UUID

/**
 * [Dao] for [AlbumOld] and [TrackOld] relationships
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface AlbumsAndTracksDao : CrossRefDao<AlbumOldAndTrackOld> {

    /**
     * Gets all albums with tracks
     * @return all albums and track relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM album")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumsWithTracks(): List<AlbumOldAndTrackOld>

    /**
     * Gets album by it's track or null if there is no album with this track
     * @param trackAlbumId id of album
     * @return found album or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM album WHERE id = :trackAlbumId")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumByTrack(trackAlbumId: UUID): AlbumOld?

    /**
     * Gets all tracks from album
     * @param albumId id of album
     * @return list with found tracks
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM track WHERE album_id = :albumId")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracksFromAlbum(albumId: UUID): List<TrackOld>
}