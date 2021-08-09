package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.TrackImage

/**
 * Dao for track - album image relationships
 */

@Dao
interface TrackImageDao {
    /**
     * Gets all tracks with their images asynchronously
     * @return all tracks with their images
     */

    @Query("SELECT * FROM image_tracks")
    suspend fun getTracksWithImages(): List<TrackImage>

    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    @Query("SELECT * FROM image_tracks WHERE track_path = :path")
    suspend fun getTrackWithImage(path: String): TrackImage?

    /** Updates track with its image asynchronously */

    @Update
    suspend fun updateTrackWithImageAsync(track: TrackImage)

    /** Adds tracks with their images asynchronously */

    @Insert
    suspend fun addTrackWithImageAsync(track: TrackImage)

    /** Removes track with its image asynchronously */

    @Delete
    suspend fun removeTrackWithImageAsync(track: TrackImage)
}