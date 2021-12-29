package com.dinaraparanid.prima.databases.entities.favourites

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/**
 * User's favourite track's entity
 */

@Entity(tableName = "favourite_tracks")
data class FavouriteTrack(
    @ColumnInfo(name = "android_id") override val androidId: Long,
    override val title: String,
    override val artist: String,
    override val playlist: String,
    @PrimaryKey override val path: String,
    override val duration: Long,
    @ColumnInfo(name = "relative_path") override val relativePath: String?,
    @ColumnInfo(name = "display_name") override val displayName: String?,
    @ColumnInfo(name = "add_date") override val addDate: Long
) : AbstractTrack(androidId, title, artist, playlist, path, duration, relativePath, displayName, addDate) {
    constructor(track: AbstractTrack) : this(
        track.androidId,
        track.title,
        track.artist,
        track.playlist,
        track.path,
        track.duration,
        track.relativePath,
        track.displayName,
        track.addDate
    )
}