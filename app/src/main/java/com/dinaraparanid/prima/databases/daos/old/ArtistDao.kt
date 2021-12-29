package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.utils.polymorphism.EntityDao
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistDao : EntityDao<ArtistOld> {
    @Query("SELECT * FROM artist")
    @Deprecated("Now using android storage instead of database")
    suspend fun getArtists(): List<ArtistOld>

    @Query("SELECT * FROM artist WHERE artist_id = :id")
    @Deprecated("Now using android storage instead of database")
    suspend fun getArtist(id: UUID): ArtistOld?

    @Query("SELECT * FROM artist WHERE name = :name")
    @Deprecated("Now using android storage instead of database")
    suspend fun getArtist(name: String): ArtistOld?
}