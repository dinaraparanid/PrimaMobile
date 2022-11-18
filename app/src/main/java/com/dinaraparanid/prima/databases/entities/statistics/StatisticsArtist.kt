package com.dinaraparanid.prima.databases.entities.statistics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Artist

/** Author's statistics */

@Entity(tableName = "statistics_artists")
data class StatisticsArtist(
    @PrimaryKey
    override val name: String,
    override val count: Long = 1,
    @ColumnInfo(name = "count_daily") override val countDaily: Long = 1,
    @ColumnInfo(name = "count_weekly") override val countWeekly: Long = 1,
    @ColumnInfo(name = "count_monthly") override val countMonthly: Long = 1,
    @ColumnInfo(name = "count_yearly") override val countYearly: Long = 1
) : Artist(name), StatisticsEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 8035458025617991247L
    }

    constructor(artist: Artist) : this(artist.name)
}