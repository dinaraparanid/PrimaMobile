package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.AlbumOld
import com.dinaraparanid.prima.databases.entities.TrackOld
import com.dinaraparanid.prima.databases.relationships.AlbumAndTrack
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface AlbumAndTrackDao {
    @Transaction
    @Query("SELECT * FROM album")
    suspend fun getAlbumsWithTracks(): List<AlbumAndTrack>

    @Query("SELECT * FROM album WHERE id = (:trackAlbumId)")
    suspend fun getAlbumByTrack(trackAlbumId: UUID): AlbumOld?

    @Query("SELECT * FROM track WHERE album_id = (:albumId)")
    suspend fun getTracksFromAlbum(albumId: UUID): List<TrackOld>
}