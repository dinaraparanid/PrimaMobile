package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.TrackOld
import com.dinaraparanid.prima.utils.polymorphism.EntityDao
import java.util.*

@Dao
@Deprecated("Now using android storage instead of database")
interface TrackDao : EntityDao<TrackOld> {
    @Query("SELECT * FROM track")
    suspend fun getTracks(): List<TrackOld>

    @Query("SELECT * FROM track WHERE track_id = (:id)")
    suspend fun getTrack(id: UUID): TrackOld?

    @Query("SELECT * FROM track WHERE title = (:title)")
    suspend fun getTrack(title: String): TrackOld?
}