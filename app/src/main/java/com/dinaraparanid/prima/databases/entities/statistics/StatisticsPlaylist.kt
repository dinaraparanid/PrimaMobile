package com.dinaraparanid.prima.databases.entities.statistics

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import java.io.Serializable

/** Playlist for statistics */

class StatisticsPlaylist(
    override val title: String,
    override val type: PlaylistType,

    // How many times tracks from this playlist have been played
    val count: Long = 0,
    @ColumnInfo(name = "count_daily") val countDaily: Long = 0,
    @ColumnInfo(name = "count_weekly") val countWeekly: Long = 0,
    @ColumnInfo(name = "count_monthly") val countMonthly: Long = 0,
    @ColumnInfo(name = "count_yearly") val countYearly: Long = 0,
    vararg tracks: AbstractTrack
) : AbstractPlaylist(title, type, *tracks) {

    /**
     * Entity itself. The only reason for using it
     * instead of [StatisticsPlaylist] itself is that
     * Room ORM badly works with the inheritance
     */

    @androidx.room.Entity(tableName = "statistics_playlists")
    class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val title: String,
        val type: Int,
        val count: Long,
        @ColumnInfo(name = "count_daily") val countDaily: Long = 0,
        @ColumnInfo(name = "count_weekly") val countWeekly: Long = 0,
        @ColumnInfo(name = "count_monthly") val countMonthly: Long = 0,
        @ColumnInfo(name = "count_yearly") val countYearly: Long = 0
    ) : Serializable {
        /** Serializable list of [StatisticsPlaylist]'s Entities */
        internal class EntityList(val entities: List<Entity>) : Serializable
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
