package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.core.AlbumOld
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface AlbumDao {
    @Query("SELECT * FROM album")
    fun getAlbums(): LiveData<List<AlbumOld>>

    @Query("SELECT * FROM album WHERE id = (:id)")
    fun getAlbum(id: UUID): LiveData<AlbumOld?>

    @Query("SELECT * FROM album WHERE title = (:title)")
    fun getAlbum(title: String): LiveData<AlbumOld?>
}