package com.dinaraparanid.prima.databases.daos.statistics

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsTrack
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/** DAO for tracks with statistics */

@Dao
interface StatisticsTrackDao : EntityDao<StatisticsTrack> {
    /**
     * Gets all tracks with statistics asynchronously
     * @return all tracks with statistics
     */

    @Query("SELECT * FROM statistics_tracks")
    suspend fun getTracksAsync(): List<StatisticsTrack>

    /**
     * Gets track by it's path asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track or null if it isn't exists
     */

    @Query("SELECT * FROM statistics_tracks WHERE path = :path")
    suspend fun getTrackAsync(path: String): StatisticsTrack?

    /**
     * Updates track's title, artist, album and count by track's path
     * @param path path to track's location in the storage
     * @param title new title
     * @param artist new artist's name
     * @param album new album's title
     */

    @Query("UPDATE statistics_tracks SET title = :title, artist = :artist, playlist = :album, count = :count, count_daily = :countDaily, count_weekly = :countWeekly, count_monthly = :countMonthly, count_yearly = :countYearly WHERE path = :path")
    suspend fun updateTrackAsync(
        path: String,
        title: String,
        artist: String,
        album: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    )
}