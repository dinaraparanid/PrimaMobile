package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.core.AlbumOld
import com.dinaraparanid.prima.core.TrackOld
import com.dinaraparanid.prima.database.relationships.AlbumAndTrack
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface AlbumAndTrackDao {
    @Transaction
    @Query("SELECT * FROM album")
    fun getAlbumsWithTracks(): LiveData<List<AlbumAndTrack>>

    @Query("SELECT * FROM album WHERE id = (:trackAlbumId)")
    fun getAlbumByTrack(trackAlbumId: UUID): LiveData<AlbumOld?>

    @Query("SELECT * FROM track WHERE album_id = (:albumId)")
    fun getTracksFromAlbum(albumId: UUID): LiveData<List<TrackOld>>
}