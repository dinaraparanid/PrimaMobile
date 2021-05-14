package com.app.musicplayer.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.app.musicplayer.core.Track
import java.util.UUID

@Dao
interface TrackDao {
    @Query("SELECT * FROM track")
    fun getTracks(): LiveData<List<Track>>

    @Query("SELECT * FROM track WHERE track_id = (:id)")
    fun getTrack(id: UUID): LiveData<Track?>

    @Query("SELECT * FROM track WHERE title = (:title)")
    fun getTrack(title: String): LiveData<Track?>

    @Update
    fun updateTrack(track: Track)

    @Insert
    fun addTrack(track: Track)
}