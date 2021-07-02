package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dinaraparanid.prima.databases.entities.ArtistOld
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistDao {
    @Query("SELECT * FROM artist")
    suspend fun getArtists(): List<ArtistOld>

    @Query("SELECT * FROM artist WHERE artist_id = (:id)")
    suspend fun getArtist(id: UUID): ArtistOld?

    @Query("SELECT * FROM artist WHERE name = (:name)")
    suspend fun getArtist(name: String): ArtistOld?

    @Update
    suspend fun updateArtist(artist: ArtistOld)

    @Insert
    suspend fun addArtist(artist: ArtistOld)
}