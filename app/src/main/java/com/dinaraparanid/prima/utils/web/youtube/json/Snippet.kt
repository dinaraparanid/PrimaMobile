package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
class Snippet(
    @Expose
    @SerializedName("publishedAt")
    val publishedAt: String,

    @Expose
    @SerializedName("channelId")
    val channelId: String,

    @Expose
    @SerializedName("title")
    val title: String,

    @Expose
    @SerializedName("description")
    val description: String,

    @Expose
    @SerializedName("thumbnails")
    val thumbnails: Thumbnails,

    @Expose
    @SerializedName("channelTitle")
    val channelTitle: String,

    @Expose
    @SerializedName("liveBroadcastContent")
    val liveBroadcastContent: String,

    @Expose
    @SerializedName("publishTime")
    val publishTime: String
)