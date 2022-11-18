package com.dinaraparanid.prima.databases.daos.covers

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.covers.TrackCover
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for track's covers */

@Dao
interface TrackCoversDao : EntityDao<TrackCover> {
    /**
     * Gets track with its cover asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with cover or null if it isn't exists
     */

    @Query("SELECT * FROM tracks_covers WHERE track_path = :path")
    suspend fun getTrackWithCover(path: String): TrackCover?

    /**
     * Removes track with its cover asynchronously
     * @param path path of track (DATA column from MediaStore)
     */

    @Query("DELETE FROM tracks_covers WHERE track_path = :path")
    suspend fun removeTrackWithCover(path: String)
}