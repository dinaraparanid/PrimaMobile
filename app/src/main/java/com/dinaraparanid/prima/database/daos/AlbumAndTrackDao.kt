package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.core.Album
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.relationships.AlbumAndTrack
import java.util.UUID

@Dao
interface AlbumAndTrackDao {
    @Transaction
    @Query("SELECT * FROM album")
    fun getAlbumsWithTracks(): LiveData<List<AlbumAndTrack>>

    @Query("SELECT * FROM album WHERE id = (:trackAlbumId)")
    fun getAlbumByTrack(trackAlbumId: UUID): LiveData<Album?>

    @Query("SELECT * FROM track WHERE album_id = (:albumId)")
    fun getTracksFromAlbum(albumId: UUID): LiveData<List<Track>>
}