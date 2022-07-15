package com.dinaraparanid.prima.databases.daos.old

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao
import java.util.UUID

/**
 * [Dao] for [ArtistsDao] entity
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface ArtistsDao : EntityDao<ArtistOld> {

    /**
     * Gets all artists
     * @return list with all artists
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM artist")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtists(): List<ArtistOld>

    /**
     * Gets artist by his id or null if he wasn't found
     * @param id id of artist
     * @return found artist or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM artist WHERE artist_id = :id")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtist(id: UUID): ArtistOld?

    /**
     * Gets artist by his name or null if he wasn't found
     * @param name name of album
     * @return found artist or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM artist WHERE name = :name")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtist(name: String): ArtistOld?
}