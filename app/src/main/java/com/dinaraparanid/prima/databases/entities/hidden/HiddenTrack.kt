package com.dinaraparanid.prima.databases.entities.hidden

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.entities.Track

/** Hidden track's entity */

@Entity(tableName = "hidden_tracks")
data class HiddenTrack(
    /** _ID from media columns */
    @ColumnInfo(name = "android_id") override val androidId: Long,

    /** TITLE from media columns */
    override val title: String,

    /** ARTIST from media columns */
    @ColumnInfo(name = "artist_name") override val artist: String,

    /** ALBUM from media columns */
    @ColumnInfo(name = "album_title") override val album: String,

    /** DATA from media columns */
    @PrimaryKey override val path: String,

    /** DURATION from media columns */
    override val duration: Long,

    /** RELATIVE_PATH from media columns */
    @ColumnInfo(name = "relative_path") override val relativePath: String?,

    /** DISPLAY_NAME from media columns */
    @ColumnInfo(name = "display_name") override val displayName: String?,

    /** DATE_ADDED from media columns */
    @ColumnInfo(name = "add_date") override val addDate: Long,

    /** TRACK from media columns */
    @ColumnInfo(name = "track_number_in_album") override val trackNumberInAlbum: Byte
) : Track(
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
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 5832500338708105093L
    }

    /** Compares track by it's [path] */
    override fun equals(other: Any?) = super.equals(other)

    /** Hashes [DefaultTrack] by it's [path] */
    override fun hashCode() = path.hashCode()

    constructor(track: Track) : this(
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