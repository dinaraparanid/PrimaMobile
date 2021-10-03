package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
class YouTubeSearchResult(
    @Expose
    @JvmField
    val kind: String,

    @Expose
    @JvmField
    val etag: String,

    @Expose
    @JvmField
    @SerializedName("nextPageToken")
    val nextPageToken: String,

    @Expose
    @JvmField
    @SerializedName("regionCode")
    val regionCode: String,

    @Expose
    @JvmField
    @SerializedName("pageInfo")
    val pageInfo: PageInfo,

    @Expose
    @JvmField
    val items: Array<FoundVideo>
)