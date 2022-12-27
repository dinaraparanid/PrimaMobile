package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.daos.EntityDao
import java.util.UUID

/**
 * [Dao] for [TrackOld] entity
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface TracksDao : EntityDao<TrackOld> {

    /**
     * Gets all tracks
     * @return list with all tracks
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM track")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTracks(): List<TrackOld>

    /**
     * Gets track by its id or null if it wasn't found
     * @param id id of track
     * @return found track or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM track WHERE track_id = :id")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTrack(id: UUID): TrackOld?

    /**
     * Gets track by its title or null if he wasn't found
     * @param title title of track
     * @return found title or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM track WHERE title = :title")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getTrack(title: String): TrackOld?
}