package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
class YouTubeSearchResult(
    @Expose
    @SerializedName("kind")
    val kind: String,

    @Expose
    @SerializedName("etag")
    val etag: String,

    @Expose
    @SerializedName("nextPageToken")
    val nextPageToken: String,

    @Expose
    @SerializedName("regionCode")
    val regionCode: String,

    @Expose
    @SerializedName("pageInfo")
    val pageInfo: PageInfo,

    @Expose
    @SerializedName("items")
    val items: Array<FoundVideo>
)