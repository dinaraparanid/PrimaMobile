package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ClientTimestamps(
    @Expose
    @JvmField
    @SerializedName("updated_by_human_at")
    val updatedByHumanAt: Long,

    @Expose
    @JvmField
    @SerializedName("lyrics_updated_at")
    val lyricsUpdatedAt: Long
)