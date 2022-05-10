package com.dinaraparanid.prima.databases.daos.images

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.images.TrackImage
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for track's covers */

@Dao
interface TrackImageDao : EntityDao<TrackImage> {
    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    @Query("SELECT * FROM image_tracks WHERE track_path = :path")
    suspend fun getTrackWithImage(path: String): TrackImage?

    /**
     * Removes track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     */

    @Query("DELETE FROM image_tracks WHERE track_path = :path")
    suspend fun removeTrackWithImage(path: String)
}