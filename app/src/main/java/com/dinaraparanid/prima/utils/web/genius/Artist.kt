package com.dinaraparanid.prima.utils.web.genius

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data about artist of searched track
 */

class Artist(
    @Expose
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @SerializedName("header_image_url")
    val headerImageUrl: String,

    @Expose
    @SerializedName("id")
    val id: Long,

    @Expose
    @SerializedName("image_url")
    val imageUrl: String,

    @Expose
    @SerializedName("is_meme_verified")
    val isMemeVerified: Boolean,

    @Expose
    @SerializedName("is_verified")
    val isVerified: Boolean,

    @Expose
    @SerializedName("name")
    val name: String,

    @Expose
    @SerializedName("url")
    val url: String,
) : Serializable