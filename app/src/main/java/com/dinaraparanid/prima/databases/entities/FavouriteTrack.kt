package com.dinaraparanid.prima.databases.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Track

@Entity(tableName = "favourite_tracks")
data class FavouriteTrack(
    override val title: String,
    override val artist: String,
    override val playlist: String,
    @PrimaryKey override val path: String,
    override val duration: Long,
) : Track(title, artist, playlist, path, duration) {
    constructor(track: Track) : this(
        track.title,
        track.artist,
        track.playlist,
        track.path,
        track.duration,
    )
}