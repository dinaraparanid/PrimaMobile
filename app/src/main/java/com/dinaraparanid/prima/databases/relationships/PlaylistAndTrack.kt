package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefEntity

/**
 * Relationships between [CustomPlaylist.Entity]
 * and its [CustomPlaylistTrack]
 */

data class PlaylistAndTrack(
    @Embedded val playlist: CustomPlaylist.Entity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlist_id",
        entity = CustomPlaylistTrack::class
    )
    val track: CustomPlaylistTrack
) : CrossRefEntity