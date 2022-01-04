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

    @Query("SELECT * FROM statistics_artists")
    suspend fun getArtistsAsync(): List<StatisticsArtist>

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    @Query("SELECT * FROM statistics_artists WHERE name = :name")
    suspend fun getArtistAsync(name: String): StatisticsArtist?

    /**
     * Updates artist's count by its path
     * @param name artist's name
     */

    @Query("UPDATE statistics_artists SET count = :count, count_daily = :countDaily, count_weekly = :countWeekly, count_monthly = :countMonthly, count_yearly = :countYearly WHERE name = :name")
    suspend fun updateArtistAsync(
        name: String,
        count: Long,
        countDaily: Long,
        countWeekly: Long,
        countMonthly: Long,
        countYearly: Long
    )

    /** Clears all counting statistics for all artists */
    @Query("UPDATE statistics_artists SET count = 0, count_daily = 0, count_weekly = 0, count_monthly = 0, count_yearly = 0")
    suspend fun clearAllArtistsStatisticsAsync()

    /** Refreshes daily statistics for all artists */
    @Query("UPDATE statistics_artists SET count_daily = 0")
    suspend fun refreshArtistsCountingDailyAsync()

    /** Refreshes weekly statistics for all artists */
    @Query("UPDATE statistics_artists SET count_weekly = 0")
    suspend fun refreshArtistsCountingWeeklyAsync()

    /** Refreshes monthly statistics for all artists */
    @Query("UPDATE statistics_artists SET count_monthly = 0")
    suspend fun refreshArtistsCountingMonthlyAsync()

    /** Refreshes yearly statistics for all artists */
    @Query("UPDATE statistics_artists SET count_yearly = 0")
    suspend fun refreshArtistsCountingYearlyAsync()

    /**
     * Increments counting statistics for a certain artist
     * @param name artist's name which statistics should be updated
     */

    @Query("UPDATE statistics_artists SET count = count + 1, count_daily = count_daily + 1, count_weekly = count_weekly + 1, count_monthly = count_monthly + 1, count_yearly = count_yearly + 1 WHERE name = :name")
    suspend fun incrementArtistCountingAsync(name: String)

    /** Gets artist with the largest count param */
    @Query("SELECT * FROM statistics_artists WHERE count = (SELECT MAX(count) from statistics_artists)")
    suspend fun getMaxCountingArtist(): StatisticsArtist

    /** Gets artist with the largest daily count param */
    @Query("SELECT * FROM statistics_artists WHERE count_daily = (SELECT MAX(count_daily) from statistics_artists)")
    suspend fun getMaxCountingArtistDaily(): StatisticsArtist

    /** Gets artist with the largest weekly count param */
    @Query("SELECT * FROM statistics_artists WHERE count_weekly = (SELECT MAX(count_weekly) from statistics_artists)")
    suspend fun getMaxCountingArtistWeekly(): StatisticsArtist

    /** Gets artist with the largest monthly count param */
    @Query("SELECT * FROM statistics_artists WHERE count_monthly = (SELECT MAX(count_monthly) from statistics_artists)")
    suspend fun getMaxCountingArtistMonthly(): StatisticsArtist

    /** Gets artist with the largest yearly count param */
    @Query("SELECT * FROM statistics_artists WHERE count_yearly = (SELECT MAX(count_yearly) from statistics_artists)")
    suspend fun getMaxCountingArtistYearly(): StatisticsArtist

    /** Removes all records from the table */
    @Query("DELETE FROM statistics_artists")
    suspend fun clearTable()
}