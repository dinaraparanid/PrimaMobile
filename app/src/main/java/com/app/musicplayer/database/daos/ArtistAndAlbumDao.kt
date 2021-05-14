package com.app.musicplayer.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.app.musicplayer.core.Album
import com.app.musicplayer.core.Artist
import com.app.musicplayer.database.relationships.ArtistAndAlbum
import java.util.UUID

@Dao
interface ArtistAndAlbumDao {
    @Transaction
    @Query("SELECT * FROM artist")
    fun getArtistsWithAlbums(): LiveData<List<ArtistAndAlbum>>

    @Query("SELECT * FROM artist WHERE artist_id = (:albumArtistId)")
    fun getArtistByAlbum(albumArtistId: UUID): LiveData<Artist?>

    @Query("SELECT * FROM album WHERE artist_id = (:artistId)")
    fun getAlbumsByArtist(artistId: UUID): LiveData<List<Album>>
}