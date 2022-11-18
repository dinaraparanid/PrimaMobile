package com.dinaraparanid.prima.databases.entities.favourites

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** User's favourite track's entity */

@Entity(tableName = "favourite_tracks")
data class FavouriteTrack(
    /** _ID from media columns */
    @ColumnInfo(name = "android_id") override val androidId: Long,

    /** TITLE from media columns */
    override val title: String,

    /** ARTIST from media columns */
    override val artist: String,

    /** ALBUM from media columns */
    override val album: String,

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
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -2102676343075916939L
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