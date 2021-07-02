package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dinaraparanid.prima.databases.entities.TrackOld
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface TrackDao {
    @Query("SELECT * FROM track")
    suspend fun getTracks(): List<TrackOld>

    @Query("SELECT * FROM track WHERE track_id = (:id)")
    suspend fun getTrack(id: UUID): TrackOld?

    @Query("SELECT * FROM track WHERE title = (:title)")
    suspend fun getTrack(title: String): TrackOld?

    @Update
    suspend fun updateTrack(track: TrackOld)

    @Insert
    suspend fun addTrack(track: TrackOld)
}