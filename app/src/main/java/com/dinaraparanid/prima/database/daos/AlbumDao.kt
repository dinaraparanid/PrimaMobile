package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.core.Album
import java.util.UUID

@Dao
interface AlbumDao {
    @Query("SELECT * FROM album")
    fun getAlbums(): LiveData<List<Album>>

    @Query("SELECT * FROM album WHERE id = (:id)")
    fun getAlbum(id: UUID): LiveData<Album?>

    @Query("SELECT * FROM album WHERE title = (:title)")
    fun getAlbum(title: String): LiveData<Album?>
}