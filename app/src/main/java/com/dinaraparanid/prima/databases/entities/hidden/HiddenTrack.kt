package com.dinaraparanid.prima.databases.entities.hidden

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Hidden track's entity */

@Entity(tableName = "HiddenTracks")
data class HiddenTrack(
    @ColumnInfo(name = "android_id") override val androidId: Long,
    override val title: String,
    @ColumnInfo(name = "artist_name") override val artist: String,
    @ColumnInfo(name = "album_title") override val album: String,
    @PrimaryKey override val path: String,
    override val duration: Long,
    @ColumnInfo(name = "relative_path") override val relativePath: String?,
    @ColumnInfo(name = "display_name") override val displayName: String?,
    @ColumnInfo(name = "add_date") override val addDate: Long,
    @ColumnInfo(name = "track_number_in_album") override val trackNumberInAlbum: Byte
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
) {
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = path.hashCode()

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