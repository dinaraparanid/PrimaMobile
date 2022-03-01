package com.dinaraparanid.prima.databases.entities.favourites

import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import java.io.Serializable

/** User's favourite playlist */

class FavouritePlaylist(
    title: String,
    override val type: PlaylistType,
    vararg tracks: AbstractTrack
) : AbstractPlaylist(title.trim(), type, *tracks) {
    override val title = title.trim()

    /**
     * Entity itself. The only reason for using it
     * instead of [FavouritePlaylist] itself is that
     * Room ORM badly works with the inheritance
     */

    @androidx.room.Entity(tableName = "favourite_playlists")
    data class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val title: String,
        val type: Int
    ) : Serializable {
        /** Serializable list of [FavouritePlaylist]'s Entities */
        internal class EntityList(val entities: List<Entity>) : Serializable
    }

    constructor(ent: Entity) : this(ent.title, PlaylistType.values()[ent.type])
}