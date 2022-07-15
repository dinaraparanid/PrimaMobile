package com.dinaraparanid.prima.databases.entities.hidden

import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

class HiddenPlaylist(
    title: String,
    override val type: PlaylistType,
    vararg tracks: AbstractTrack
) : AbstractPlaylist(title.trim(), type, *tracks) {
    override val title = title.trim()

    /**
     * Entity itself. The only reason for using it
     * instead of [HiddenPlaylist] itself is that
     * Room ORM badly works with the inheritance
     */

    @androidx.room.Entity(tableName = "HiddenPlaylists")
    data class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val title: String,
        val type: Int
    ) : com.dinaraparanid.prima.utils.polymorphism.databases.Entity {
        constructor(playlist: AbstractPlaylist) : this(
            id = 0,
            title = playlist.title,
            type = playlist.type.ordinal
        )
    }

    constructor(ent: Entity) : this(ent.title, PlaylistType.values()[ent.type])
}