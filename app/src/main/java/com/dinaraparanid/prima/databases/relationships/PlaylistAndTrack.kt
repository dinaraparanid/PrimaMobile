package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack

/**
 * Relationship between user's playlist and track
 */

data class PlaylistAndTrack(
    @Embedded val playlist: CustomPlaylist.Entity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlist_id",
        entity = CustomPlaylistTrack::class
    )
    val tracks: List<CustomPlaylistTrack>
)
