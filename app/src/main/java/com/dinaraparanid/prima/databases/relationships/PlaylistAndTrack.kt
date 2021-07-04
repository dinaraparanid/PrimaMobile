package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack

data class PlaylistAndTrack(
    @Embedded val playlist: CustomPlaylist.Entity,
    @Relation(
        parentColumn = "title",
        entityColumn = "playlist_title"
    )
    val track: CustomPlaylistTrack?
)
