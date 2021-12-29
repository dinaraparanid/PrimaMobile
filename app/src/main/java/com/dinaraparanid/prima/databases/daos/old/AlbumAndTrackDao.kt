package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.relationships.AlbumAndTrack
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface AlbumAndTrackDao {
    @Transaction
    @Query("SELECT * FROM album")
    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumsWithTracks(): List<AlbumAndTrack>

    @Query("SELECT * FROM album WHERE id = :trackAlbumId")
    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumByTrack(trackAlbumId: UUID): AlbumOld?

    @Query("SELECT * FROM track WHERE album_id = :albumId")
    @Deprecated("Now using android storage instead of database")
    suspend fun getTracksFromAlbum(albumId: UUID): List<TrackOld>
}