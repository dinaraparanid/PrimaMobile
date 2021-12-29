package com.dinaraparanid.prima.databases.daos.statistics

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsPlaylist
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
}