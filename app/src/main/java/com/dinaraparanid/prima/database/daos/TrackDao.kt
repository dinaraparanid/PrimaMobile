package com.dinaraparanid.prima.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dinaraparanid.prima.core.TrackOld
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface TrackDao {
    @Query("SELECT * FROM track")
    fun getTracks(): LiveData<List<TrackOld>>

    @Query("SELECT * FROM track WHERE track_id = (:id)")
    fun getTrack(id: UUID): LiveData<TrackOld?>

    @Query("SELECT * FROM track WHERE title = (:title)")
    fun getTrack(title: String): LiveData<TrackOld?>

    @Update
    fun updateTrack(track: TrackOld)

    @Insert
    fun addTrack(track: TrackOld)
}