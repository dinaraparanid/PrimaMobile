package com.dinaraparanid.prima.databases.entities.hidden

import androidx.room.PrimaryKey
import androidx.room.Entity as RoomEntity
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.databases.entities.Entity as PrimaEntity

/** Hidden playlist's entity */

class HiddenPlaylist(
    title: String,
    type: PlaylistType,
    vararg tracks: Track
) : AbstractPlaylist(title.trim(), type, *tracks) {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 9049070791956913899L
    }

    override val title = title.trim()

    /**
     * Entity itself. The only reason for using it
     * instead of [HiddenPlaylist] itself is that
     * Room ORM badly works with the inheritance
     */

    @RoomEntity(tableName = "hidden_playlists")
    data class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val title: String,
        val type: Int
    ) : PrimaEntity {
        private companion object {
            /** UID required to serialize */
            private const val serialVersionUID = 8063627517553969525L
        }

        constructor(playlist: AbstractPlaylist) : this(
            id = 0,
            title = playlist.title,
            type = playlist.type.ordinal
        )
    }

    constructor(ent: Entity) : this(ent.title, PlaylistType.values()[ent.type])
}