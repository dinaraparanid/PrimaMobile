package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
class Id(
    @Expose
    @SerializedName("kind")
    val kind: String,

    @Expose
    @SerializedName("videoId")
    val videoId: String
)