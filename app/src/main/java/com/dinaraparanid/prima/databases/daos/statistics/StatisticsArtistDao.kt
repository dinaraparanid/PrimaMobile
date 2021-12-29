package com.dinaraparanid.prima.databases.daos.statistics

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.statistics.StatisticsArtist
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/** DAO for artists with statistics */

@Dao
interface StatisticsArtistDao : EntityDao<StatisticsArtist> {
    /**
     * Gets all artists with statistics asynchronously
     * @return all artists with statistics
     */

    @Query("SELECT * FROM statistics_artist")
    suspend fun getArtistsAsync(): List<StatisticsArtist>

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    @Query("SELECT * FROM statistics_artist WHERE name = :name")
    suspend fun getArtistAsync(name: String): StatisticsArtist?

    /**
     * Updates artist's count by its path
     * @param name artist's name
     */

    @Query("UPDATE statistics_artist SET count = :count, count_daily = :countDaily, count_weekly = :countWeekly, count_monthly = :countMonthly, count_yearly = :countYearly WHERE name = :name")
    suspend fun updateTrackAsync(
        name: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    )
}