package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefEntity

/**
 * Relationships between [CustomPlaylist.Entity]
 * and its all [CustomPlaylistTrack]
 */

data class PlaylistWithTracks(
    @Embedded val playlist: CustomPlaylist.Entity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlist_id",
        entity = CustomPlaylistTrack::class
    )
    val tracks: List<CustomPlaylistTrack>
) : CrossRefEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 846537785013297093L
    }
}
