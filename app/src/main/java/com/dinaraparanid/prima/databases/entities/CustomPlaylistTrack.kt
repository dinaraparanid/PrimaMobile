package com.dinaraparanid.prima.databases.entities

import androidx.room.*
import com.dinaraparanid.prima.core.Track

@Entity(
    tableName = "CustomTracks", foreignKeys = [ForeignKey(
        entity = CustomPlaylist.Entity::class,
        parentColumns = arrayOf("title"),
        childColumns = arrayOf("playlist_title"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class CustomPlaylistTrack(
    @PrimaryKey(autoGenerate = true) val id: Long,
    override val title: String,
    @ColumnInfo(name = "artist_name") override val artist: String,
    @ColumnInfo(name = "playlist_title") override val playlist: String,
    override val path: String,
    override val duration: Long,
) : Track(title, artist, playlist, path, duration)
