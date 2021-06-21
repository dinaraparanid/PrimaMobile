package com.dinaraparanid.prima.core

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_tracks")
data class FavouriteTrack(
    override val title: String,
    override val artist: String,
    override val album: String,
    @PrimaryKey override val path: String,
    override val duration: Long,
    @ColumnInfo(name = "album_id", index = true) override val albumId: Long
) : Track(title, artist, album, path, duration, albumId) {
    constructor(track: Track) : this(
        track.title,
        track.artist,
        track.album,
        track.path,
        track.duration,
        track.albumId
    )
}