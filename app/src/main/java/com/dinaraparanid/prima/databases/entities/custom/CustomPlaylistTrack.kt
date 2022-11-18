package com.dinaraparanid.prima.databases.entities.custom

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** CustomPlaylist's track's entity */

@Entity(
    tableName = "custom_tracks", foreignKeys = [
        ForeignKey(
            entity = CustomPlaylist.Entity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("playlist_id"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomPlaylist.Entity::class,
            parentColumns = arrayOf("title"),
            childColumns = arrayOf("playlist_title"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomPlaylistTrack(
    /** _ID from media columns */
    @ColumnInfo(name = "android_id") override val androidId: Long,

    /** Database's id for this entity */
    @PrimaryKey(autoGenerate = true) val id: Long,

    /** TITLE from media columns */
    override val title: String,

    /** ARTIST from media columns */
    @ColumnInfo(name = "artist_name") override val artist: String,

    /** ALBUM from media columns */
    @ColumnInfo(name = "album_title", index = true) override val album: String,

    /** [CustomPlaylist.Entity.id] */
    @ColumnInfo(name = "playlist_id") val playlistId: Long,

    /** [CustomPlaylist.Entity.title] */
    @ColumnInfo(name = "playlist_title") val playlistTitle: String,

    /** DATA from media columns */
    override val path: String,

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
        private const val serialVersionUID = -8685800246002466981L
    }

    /** Compares track by its [id] (if [other] is [CustomPlaylistTrack]) or [path] */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractTrack) return false
        if (other is CustomPlaylistTrack) return id == other.id
        return path == other.path
    }

    /** Hashes [DefaultTrack] by it's [path] and [id] */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}
