package com.dinaraparanid.prima.utils.web.genius

import com.dinaraparanid.prima.utils.extensions.fixedImageUrl
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data about artist of searched track
 */

class Artist(
    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    @SerializedName("header_image_url")
    val _headerImageUrl: String,

    @Expose
    @JvmField
    @SerializedName("id")
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("image_url")
    val _imageUrl: String,

    @Expose
    @JvmField
    @SerializedName("is_meme_verified")
    val isMemeVerified: Boolean,

    @Expose
    @JvmField
    @SerializedName("is_verified")
    val isVerified: Boolean,

    @Expose
    @JvmField
    @SerializedName("name")
    val name: String,

    @Expose
    @JvmField
    @SerializedName("url")
    val url: String,
) : Serializable {
    internal inline val headerImageUrl
        get() = _headerImageUrl.fixedImageUrl

    internal inline val imageUrl
        get() = _imageUrl.fixedImageUrl
}