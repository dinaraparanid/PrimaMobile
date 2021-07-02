package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.AlbumOld
import com.dinaraparanid.prima.databases.entities.ArtistOld
import com.dinaraparanid.prima.databases.relationships.ArtistAndAlbum
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistAndAlbumDao {
    @Transaction
    @Query("SELECT * FROM artist")
    suspend fun getArtistsWithAlbums(): List<ArtistAndAlbum>

    @Query("SELECT * FROM artist WHERE artist_id = (:albumArtistId)")
    suspend fun getArtistByAlbum(albumArtistId: UUID): ArtistOld?

    @Query("SELECT * FROM album WHERE artist_id = (:artistId)")
    suspend fun getAlbumsByArtist(artistId: UUID): List<AlbumOld>
}