package com.dinaraparanid.prima.databases.entities.custom

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** CustomPlaylist's track's entity */

@Entity(
    tableName = "CustomTracks", foreignKeys = [
        ForeignKey(
            entity = CustomPlaylist.Entity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("playlist_id"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomPlaylist.Entity::class,
            parentColumns = arrayOf("title"),
            childColumns = arrayOf("playlist_title"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomPlaylistTrack(
    @ColumnInfo(name = "android_id") override val androidId: Long,
    @PrimaryKey(autoGenerate = true) val id: Long,
    override val title: String,
    @ColumnInfo(name = "artist_name") override val artist: String,
    @ColumnInfo(name = "album_title", index = true) override val album: String,
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "playlist_title") val playlistTitle: String,
    override val path: String,
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractTrack) return false
        if (other is CustomPlaylistTrack) return id == other.id
        return path == other.path
    }

    override fun hashCode(): Int = super.hashCode()
}
