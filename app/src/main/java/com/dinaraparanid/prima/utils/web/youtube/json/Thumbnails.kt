package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
class Thumbnails(
    @Expose
    @SerializedName("default")
    val default: Thumbnail,

    @Expose
    @SerializedName("medium")
    val medium: Thumbnail,

    @Expose
    @SerializedName("high")
    val high: Thumbnail,
)