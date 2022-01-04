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

    /** Clears all counting statistics for all tracks */
    @Query("UPDATE statistics_tracks SET count = 0, count_daily = 0, count_weekly = 0, count_monthly = 0, count_yearly = 0")
    suspend fun clearAllTracksStatisticsAsync()

    /** Refreshes daily statistics for all tracks */
    @Query("UPDATE statistics_tracks SET count_daily = 0")
    suspend fun refreshTracksCountingDailyAsync()

    /** Refreshes weekly statistics for all tracks */
    @Query("UPDATE statistics_tracks SET count_weekly = 0")
    suspend fun refreshTracksCountingWeeklyAsync()

    /** Refreshes monthly statistics for all tracks */
    @Query("UPDATE statistics_tracks SET count_monthly = 0")
    suspend fun refreshTracksCountingMonthlyAsync()

    /** Refreshes yearly statistics for all tracks */
    @Query("UPDATE statistics_tracks SET count_yearly = 0")
    suspend fun refreshTracksCountingYearlyAsync()

    /**
     * Increments counting statistics for a certain track
     * @param path track's path which statistics should be updated
     */

    @Query("UPDATE statistics_tracks SET count = count + 1, count_daily = count_daily + 1, count_weekly = count_weekly + 1, count_monthly = count_monthly + 1, count_yearly = count_yearly + 1 WHERE path = :path")
    suspend fun incrementTrackCountingAsync(path: String)

    /** Gets track with the largest count param */
    @Query("SELECT * FROM statistics_tracks WHERE count = (SELECT MAX(count) from statistics_tracks)")
    suspend fun getMaxCountingTrack(): StatisticsTrack

    /** Gets track with the largest daily count param */
    @Query("SELECT * FROM statistics_tracks WHERE count_daily = (SELECT MAX(count_daily) from statistics_tracks)")
    suspend fun getMaxCountingTrackDaily(): StatisticsTrack

    /** Gets track with the largest weekly count param */
    @Query("SELECT * FROM statistics_tracks WHERE count_weekly = (SELECT MAX(count_weekly) from statistics_tracks)")
    suspend fun getMaxCountingTrackWeekly(): StatisticsTrack

    /** Gets track with the largest monthly count param */
    @Query("SELECT * FROM statistics_tracks WHERE count_monthly = (SELECT MAX(count_monthly) from statistics_tracks)")
    suspend fun getMaxCountingTrackMonthly(): StatisticsTrack

    /** Gets track with the largest yearly count param */
    @Query("SELECT * FROM statistics_tracks WHERE count_yearly = (SELECT MAX(count_yearly) from statistics_tracks)")
    suspend fun getMaxCountingTrackYearly(): StatisticsTrack

    @Query("DELETE FROM statistics_tracks")
    suspend fun clearTable()
}