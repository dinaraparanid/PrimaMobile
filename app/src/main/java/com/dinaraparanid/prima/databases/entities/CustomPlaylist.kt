package com.dinaraparanid.prima.databases.entities

import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Playlist

class CustomPlaylist(override val title: String = "No title") : Playlist(title) {

    @androidx.room.Entity(tableName = "CustomPlaylists")
    class Entity(@PrimaryKey val title: String)

    constructor(ent: Entity) : this(ent.title)

    override fun toString(): String = "CustomPlaylist(title='$title')"
}
