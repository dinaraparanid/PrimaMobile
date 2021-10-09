package com.dinaraparanid.prima.databases.entities

import androidx.room.Index
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.AbstractTrack
import java.io.Serializable

/**
 * User's playlist
 */

class CustomPlaylist(
    override val title: String = "No title",
    tracks: List<AbstractTrack>
) : Playlist(title, tracks) {

    /**
     * Entity itself. The reason for using it
     * instead of CustomPlaylist itself is that
     * Room ORM badly works with the inheritance
     */

    @androidx.room.Entity(
        tableName = "CustomPlaylists",
        indices = [Index(value = ["title"], unique = true)]
    )
    class Entity(@PrimaryKey(autoGenerate = true) val id: Long, val title: String) : Serializable {
        /**
         * Serializable list of CustomPlaylist's Entities
         */
        internal class EntityList(val entities: List<Entity>) : Serializable

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entity) return false
            return title == other.title
        }

        override fun hashCode(): Int = title.hashCode()

        operator fun component1(): Long = id
        operator fun component2(): String = title
    }

    constructor(ent: Entity) : this(ent.title)
    constructor(title: String) : this(title, mutableListOf())

    override fun toString(): String = "CustomPlaylist(title='$title')"
}
