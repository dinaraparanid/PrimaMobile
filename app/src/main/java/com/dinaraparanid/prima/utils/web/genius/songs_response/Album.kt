package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.dinaraparanid.prima.utils.web.genius.Artist
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Album(
    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    @SerializedName("cover_art_url")
    val coverArtUrl: String,

    @Expose
    @JvmField
    @SerializedName("full_title")
    val fullTitle: String,

    @Expose
    @JvmField
    val id: String,

    @Expose
    @JvmField
    val name: String,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    val artist: Artist
)