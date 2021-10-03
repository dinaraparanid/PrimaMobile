package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SongRelationship(
    @Expose
    @JvmField
    @SerializedName("relationship_type")
    val relationshipType: String,

    @Expose
    @JvmField
    @SerializedName("type")
    val type: String,

    @Expose
    @JvmField
    @SerializedName("songs")
    val songs: Array<GeniusTrack>
)