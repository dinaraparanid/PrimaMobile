package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.relationships.ArtistAndAlbum
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistAndAlbumDao {
    @Transaction
    @Query("SELECT * FROM artist")
    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistsWithAlbums(): List<ArtistAndAlbum>

    @Query("SELECT * FROM artist WHERE artist_id = :albumArtistId")
    @Deprecated("Now using android storage instead of database")
    suspend fun getArtistByAlbum(albumArtistId: UUID): ArtistOld?

    @Query("SELECT * FROM album WHERE artist_id = :artistId")
    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbumsByArtist(artistId: UUID): List<AlbumOld>
}