package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao
import java.util.UUID

/**
 * [Dao] for [AlbumOld] entity
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface AlbumsDao : EntityDao<AlbumOld> {

    /**
     * Gets all albums
     * @return list with all albums
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM album")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbums(): List<AlbumOld>

    /**
     * Gets album by its id or null if it wasn't found
     * @param id id of album
     * @return found album or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM album WHERE id = :id")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbum(id: UUID): AlbumOld?

    /**
     * Gets album by its title or null if it wasn't found
     * @param title title of album
     * @return found album or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM album WHERE title = :title")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbum(title: String): AlbumOld?
}