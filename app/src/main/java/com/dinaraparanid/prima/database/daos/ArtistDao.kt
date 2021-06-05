package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dinaraparanid.prima.core.ArtistOld
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface ArtistDao {
    @Query("SELECT * FROM artist")
    fun getArtists(): LiveData<List<ArtistOld>>

    @Query("SELECT * FROM artist WHERE artist_id = (:id)")
    fun getArtist(id: UUID): LiveData<ArtistOld?>

    @Query("SELECT * FROM artist WHERE name = (:name)")
    fun getArtist(name: String): LiveData<ArtistOld?>

    @Update
    fun updateArtist(artist: ArtistOld)

    @Insert
    fun addArtist(artist: ArtistOld)
}