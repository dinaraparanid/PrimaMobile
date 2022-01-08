package com.dinaraparanid.prima.databases.daos.statistics

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

@Dao
interface StatisticsPlaylistDao : EntityDao<StatisticsPlaylist.Entity> {
    /**
     * Gets all playlists with statistics asynchronously
     * @return all playlists with statistics
     */

    @Query("SELECT * FROM statistics_playlists")
    suspend fun getPlaylistsAsync(): List<StatisticsPlaylist.Entity>

    /**
     * Gets playlist by its title and type (ordinal) asynchronously
     * @param title playlist's title
     * @param type ordinal of [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType]
     * @return playlist or null if it doesn't exist
     */

    @Query("SELECT * FROM statistics_playlists WHERE title = :title AND type = :type")
    suspend fun getPlaylistAsync(title: String, type: Int): StatisticsPlaylist.Entity?

    /**
     * Updates playlist's count and title by its id
     * @param id playlist's id
     * @param title playlist's title
     */

    @Query("UPDATE statistics_playlists SET title = :title, count = :count, count_daily = :countDaily, count_weekly = :countWeekly, count_monthly = :countMonthly, count_yearly = :countYearly WHERE id = :id")
    suspend fun updatePlaylistAsync(
        id: Long,
        title: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    )

    /** Clears all counting statistics for all playlists */
    @Query("UPDATE statistics_playlists SET count = 0, count_daily = 0, count_weekly = 0, count_monthly = 0, count_yearly = 0")
    suspend fun clearAllPlaylistsStatisticsAsync()

    /** Refreshes daily statistics for all playlists */
    @Query("UPDATE statistics_playlists SET count_daily = 0")
    suspend fun refreshPlaylistsCountingDailyAsync()

    /** Refreshes weekly statistics for all playlists */
    @Query("UPDATE statistics_playlists SET count_weekly = 0")
    suspend fun refreshPlaylistsCountingWeeklyAsync()

    /** Refreshes monthly statistics for all playlists */
    @Query("UPDATE statistics_playlists SET count_monthly = 0")
    suspend fun refreshPlaylistsCountingMonthlyAsync()

    /** Refreshes yearly statistics for all playlists */
    @Query("UPDATE statistics_playlists SET count_yearly = 0")
    suspend fun refreshPlaylistsCountingYearlyAsync()

    /**
     * Increments counting statistics for a certain playlist
     * @param title playlist's title
     * @param type playlist's type ordinal of [com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist.PlaylistType]
     */

    @Query("UPDATE statistics_playlists SET count = count + 1, count_daily = count_daily + 1, count_weekly = count_weekly + 1, count_monthly = count_monthly + 1, count_yearly = count_yearly + 1 WHERE title = :title AND type = :type")
    suspend fun incrementPlaylistCountingAsync(title: String, type: Int)

    /** Gets playlist with the largest count param */
    @Query("SELECT * FROM statistics_playlists WHERE count = (SELECT MAX(count) from statistics_playlists)")
    suspend fun getMaxCountingPlaylist(): StatisticsPlaylist.Entity

    /** Gets playlist with the largest daily count param */
    @Query("SELECT * FROM statistics_playlists WHERE count_daily = (SELECT MAX(count_daily) from statistics_playlists)")
    suspend fun getMaxCountingPlaylistDaily(): StatisticsPlaylist.Entity

    /** Gets playlist with the largest weekly count param */
    @Query("SELECT * FROM statistics_playlists WHERE count_weekly = (SELECT MAX(count_weekly) from statistics_playlists)")
    suspend fun getMaxCountingPlaylistWeekly(): StatisticsPlaylist.Entity

    /** Gets playlist with the largest monthly count param */
    @Query("SELECT * FROM statistics_playlists WHERE count_monthly = (SELECT MAX(count_monthly) from statistics_playlists)")
    suspend fun getMaxCountingPlaylistMonthly(): StatisticsPlaylist.Entity

    /** Gets playlist with the largest yearly count param */
    @Query("SELECT * FROM statistics_playlists WHERE count_yearly = (SELECT MAX(count_yearly) from statistics_playlists)")
    suspend fun getMaxCountingPlaylistYearly(): StatisticsPlaylist.Entity

    /** Removes custom playlist by its title */
    @Query("DELETE FROM statistics_playlists WHERE title = :title AND type = :type")
    suspend fun removeCustomPlaylistAsync(title: String, type: Int = AbstractPlaylist.PlaylistType.CUSTOM.ordinal)

    /** Removes all record from the table */
    @Query("DELETE FROM statistics_playlists")
    suspend fun clearTable()
}