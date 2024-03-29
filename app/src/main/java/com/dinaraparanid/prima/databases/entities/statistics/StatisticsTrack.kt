package com.dinaraparanid.prima.databases.entities.statistics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Track's statistics entity */

@Entity(tableName = "statistics_tracks")
data class StatisticsTrack(
    @ColumnInfo(name = "android_id") override val androidId: Long,
    override val title: String,
    override val artist: String,
    override val album: String,
    @PrimaryKey override val path: String,
    override val duration: Long,
    @ColumnInfo(name = "relative_path") override val relativePath: String?,
    @ColumnInfo(name = "display_name") override val displayName: String?,
    @ColumnInfo(name = "add_date") override val addDate: Long,
    @ColumnInfo(name = "track_number_in_album") override val trackNumberInAlbum: Byte,
    override val count: Long = 1,
    @ColumnInfo(name = "count_daily") override val countDaily: Long = 1,
    @ColumnInfo(name = "count_weekly") override val countWeekly: Long = 1,
    @ColumnInfo(name = "count_monthly") override val countMonthly: Long = 1,
    @ColumnInfo(name = "count_yearly") override val countYearly: Long = 1
) : AbstractTrack(
    androidId,
    title,
    artist,
    album,
    path,
    duration,
    relativePath,
    displayName,
    addDate,
    trackNumberInAlbum
), StatisticsEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 5915483358617544110L
    }

    constructor(track: AbstractTrack) : this(
        track.androidId,
        track.title,
        track.artist,
        track.album,
        track.path,
        track.duration,
        track.relativePath,
        track.displayName,
        track.addDate,
        track.trackNumberInAlbum
    )
}