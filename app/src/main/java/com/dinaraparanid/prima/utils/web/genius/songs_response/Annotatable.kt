package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Annotatable(
    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    @SerializedName("client_timestamps")
    val clientTimestamps: ClientTimestamps,

    @Expose
    @JvmField
    val context: String,

    @Expose
    @JvmField
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("image_url")
    val imageUrl: String,

    @Expose
    @JvmField
    @SerializedName("link_title")
    val linkTitle: String,

    @Expose
    @JvmField
    val title: String,

    @Expose
    @JvmField
    val type: String,

    @Expose
    @JvmField
    val url: String
) : Serializable