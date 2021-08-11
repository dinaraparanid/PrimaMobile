package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.ArtistOld
import com.dinaraparanid.prima.utils.polymorphism.EntityDao
import java.util.*

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistDao : EntityDao<ArtistOld> {
    @Query("SELECT * FROM artist")
    suspend fun getArtists(): List<ArtistOld>

    @Query("SELECT * FROM artist WHERE artist_id = (:id)")
    suspend fun getArtist(id: UUID): ArtistOld?

    @Query("SELECT * FROM artist WHERE name = (:name)")
    suspend fun getArtist(name: String): ArtistOld?
}