package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack

@Dao
interface CustomPlaylistTrackDao {
    @Query("SELECT * FROM CustomTracks")
    suspend fun getTracks(): List<CustomPlaylistTrack>

    @Query("SELECT * FROM CustomTracks WHERE path = (:id)")
    suspend fun getTrack(id: Long): CustomPlaylistTrack?

    @Update
    suspend fun updateTrack(track: CustomPlaylistTrack)

    @Insert
    suspend fun addTrack(track: CustomPlaylistTrack)

    @Delete
    suspend fun removeTrack(track: CustomPlaylistTrack)
}