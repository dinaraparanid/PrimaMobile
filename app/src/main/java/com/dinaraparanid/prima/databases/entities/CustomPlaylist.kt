package com.dinaraparanid.prima.databases.entities

import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track

class CustomPlaylist(override val title: String = "No title", tracks: List<Track>) :
    Playlist(title, tracks) {

    @androidx.room.Entity(tableName = "CustomPlaylists")
    class Entity(@PrimaryKey val title: String)

    constructor(ent: Entity) : this(ent.title)
    constructor(title: String) : this(title, mutableListOf())

    override fun toString(): String = "CustomPlaylist(title='$title')"
}
