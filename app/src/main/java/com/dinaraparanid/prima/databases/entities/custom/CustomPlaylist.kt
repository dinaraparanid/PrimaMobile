package com.dinaraparanid.prima.databases.entities.custom

import androidx.room.Index
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import java.io.Serializable

/** User's playlist */

class CustomPlaylist(
    title: String = "No title",
    vararg tracks: AbstractTrack
) : AbstractPlaylist(title.trim(), PlaylistType.CUSTOM, *tracks) {
    override val title = title.trim()

    /**
     * Entity itself. The only reason for using it
     * instead of CustomPlaylist itself is that
     * Room ORM badly works with the inheritance
     */

    @androidx.room.Entity(
        tableName = "CustomPlaylists",
        indices = [Index(value = ["title"], unique = true)]
    )
    data class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val title: String
    ) : Serializable {
        /** Serializable list of [CustomPlaylist]'s Entities */
        internal class EntityList(val entities: List<Entity>) : Serializable, Collection<Entity> {
            override val size = entities.size
            override fun contains(element: Entity) = element in entities
            override fun containsAll(elements: Collection<Entity>) = entities.containsAll(elements)
            override fun isEmpty() = entities.isEmpty()
            override fun iterator() = entities.listIterator()
        }
    }

    constructor(ent: Entity) : this(ent.title)
}
