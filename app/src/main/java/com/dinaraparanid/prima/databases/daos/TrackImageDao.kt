package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/**
 * Dao for track - album image relationships
 */

@Dao
interface TrackImageDao : EntityDao<TrackImage> {
    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    @Query("SELECT * FROM image_tracks WHERE track_path = :path")
    suspend fun getTrackWithImage(path: String): TrackImage?
}