package com.dinaraparanid.prima.databases.entities.custom

import androidx.room.Entity as RoomEntity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.databases.entities.Entity as PrimaEntity

/** User's playlist */

class CustomPlaylist(
    title: String = "No title",
    vararg tracks: Track
) : AbstractPlaylist(title.trim(), PlaylistType.CUSTOM, *tracks) {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 3412038928151099739L
    }

    override val title = title.trim()

    /**
     * Entity itself. The only reason for using it
     * instead of CustomPlaylist itself is that
     * Room ORM badly works with the inheritance
     */

    @RoomEntity(
        tableName = "custom_playlists",
        indices = [Index(value = ["title"], unique = true)]
    )
    data class Entity(
        @PrimaryKey(autoGenerate = true) val id: Long,
        val title: String
    ) : PrimaEntity {
        private companion object {
            /** UID required to serialize */
            private const val serialVersionUID = 9026175096729070488L
        }
    }

    constructor(ent: Entity) : this(ent.title)
}
