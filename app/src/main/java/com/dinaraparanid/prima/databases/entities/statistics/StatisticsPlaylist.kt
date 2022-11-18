package com.dinaraparanid.prima.databases.entities.statistics

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Playlist for statistics */

class StatisticsPlaylist(
    title: String,
    override val type: PlaylistType,
    override val count: Long = 1,
    @ColumnInfo(name = "count_daily") override val countDaily: Long = 1,
    @ColumnInfo(name = "count_weekly") override val countWeekly: Long = 1,
    @ColumnInfo(name = "count_monthly") override val countMonthly: Long = 1,
    @ColumnInfo(name = "count_yearly") override val countYearly: Long = 1,
    vararg tracks: AbstractTrack
) : AbstractPlaylist(title.trim(), type, *tracks), StatisticsEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 328554748076959892L
    }

    override val title = title.trim()

    /**
     * Entity itself. The only reason for using it
     * instead of [StatisticsPlaylist] itself is that
     * Room ORM badly works with the inheritance
     */

    @androidx.room.Entity(tableName = "statistics_playlists")
    class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val type: Int,
        override val count: Long = 1,
        @ColumnInfo(name = "count_daily") override val countDaily: Long = 1,
        @ColumnInfo(name = "count_weekly") override val countWeekly: Long = 1,
        @ColumnInfo(name = "count_monthly") override val countMonthly: Long = 1,
        @ColumnInfo(name = "count_yearly") override val countYearly: Long = 1
    ) : StatisticsEntity {
        private companion object {
            /** UID required to serialize */
            private const val serialVersionUID = -8199598255159146625L
        }
    }

    constructor(ent: Entity) : this(
        ent.title,
        PlaylistType.values()[ent.type],
        count = ent.count,
        countDaily = ent.countDaily,
        countWeekly = ent.countWeekly,
        countMonthly = ent.countMonthly,
        countYearly = ent.countYearly
    )
}
