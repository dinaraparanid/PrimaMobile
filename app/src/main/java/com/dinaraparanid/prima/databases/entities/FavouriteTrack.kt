package com.dinaraparanid.prima.databases.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Track

@Entity(tableName = "favourite_tracks")
data class FavouriteTrack(
    @ColumnInfo(name = "android_id") override val androidId: Long,
    override val title: String,
    override val artist: String,
    override val playlist: String,
    @PrimaryKey override val path: String,
    override val duration: Long,
    @ColumnInfo(name = "relative_path") override val relativePath: String?,
    @ColumnInfo(name = "display_name") override val displayName: String?
) : Track(androidId, title, artist, playlist, path, duration, relativePath, displayName) {
    constructor(track: Track) : this(
        track.androidId,
        track.title,
        track.artist,
        track.playlist,
        track.path,
        track.duration,
        track.relativePath,
        track.displayName
    )
}