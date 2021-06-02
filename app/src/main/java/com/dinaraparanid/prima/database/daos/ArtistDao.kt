package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.core.Artist
import java.util.UUID

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artist")
    fun getArtists(): LiveData<List<Artist>>

    @Query("SELECT * FROM artist WHERE artist_id = (:id)")
    fun getArtist(id: UUID): LiveData<Artist?>

    @Query("SELECT * FROM artist WHERE name = (:name)")
    fun getArtist(name: String): LiveData<Artist?>
}