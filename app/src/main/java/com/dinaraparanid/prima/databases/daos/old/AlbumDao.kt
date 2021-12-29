package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.utils.polymorphism.EntityDao
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface AlbumDao : EntityDao<AlbumOld> {
    @Query("SELECT * FROM album")
    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbums(): List<AlbumOld>

    @Query("SELECT * FROM album WHERE id = :id")
    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbum(id: UUID): AlbumOld?

    @Query("SELECT * FROM album WHERE title = :title")
    @Deprecated("Now using android storage instead of database")
    suspend fun getAlbum(title: String): AlbumOld?
}