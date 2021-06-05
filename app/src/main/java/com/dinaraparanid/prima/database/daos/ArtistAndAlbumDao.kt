package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.core.AlbumOld
import com.dinaraparanid.prima.core.ArtistOld
import com.dinaraparanid.prima.database.relationships.ArtistAndAlbum
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistAndAlbumDao {
    @Transaction
    @Query("SELECT * FROM artist")
    fun getArtistsWithAlbums(): LiveData<List<ArtistAndAlbum>>

    @Query("SELECT * FROM artist WHERE artist_id = (:albumArtistId)")
    fun getArtistByAlbum(albumArtistId: UUID): LiveData<ArtistOld?>

    @Query("SELECT * FROM album WHERE artist_id = (:artistId)")
    fun getAlbumsByArtist(artistId: UUID): LiveData<List<AlbumOld>>
}