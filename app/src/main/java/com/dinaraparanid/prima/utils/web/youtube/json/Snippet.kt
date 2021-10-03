package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
data class Snippet(
    @Expose
    @JvmField
    @SerializedName("publishedAt")
    val publishedAt: String,

    @Expose
    @JvmField
    @SerializedName("channelId")
    val channelId: String,

    @Expose
    @JvmField
    val title: String,

    @Expose
    @JvmField
    val description: String,

    @Expose
    @JvmField
    val thumbnails: Thumbnails,

    @Expose
    @JvmField
    @SerializedName("channelTitle")
    val channelTitle: String,

    @Expose
    @JvmField
    @SerializedName("liveBroadcastContent")
    val liveBroadcastContent: String,

    @Expose
    @JvmField
    @SerializedName("publishTime")
    val publishTime: String
)