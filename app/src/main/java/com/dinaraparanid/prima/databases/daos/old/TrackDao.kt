package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.utils.polymorphism.EntityDao
import java.util.UUID

@Dao
@Deprecated("Now using android storage instead of database")
interface TrackDao : EntityDao<TrackOld> {
    @Query("SELECT * FROM track")
    @Deprecated("Now using android storage instead of database")
    suspend fun getTracks(): List<TrackOld>

    @Query("SELECT * FROM track WHERE track_id = :id")
    @Deprecated("Now using android storage instead of database")
    suspend fun getTrack(id: UUID): TrackOld?

    @Query("SELECT * FROM track WHERE title = :title")
    @Deprecated("Now using android storage instead of database")
    suspend fun getTrack(title: String): TrackOld?
}