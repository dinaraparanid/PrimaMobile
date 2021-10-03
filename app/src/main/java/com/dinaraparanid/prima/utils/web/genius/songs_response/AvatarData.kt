package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AvatarData(
    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    @SerializedName("bounding_box")
    val boundingBox: BoundingBox
)