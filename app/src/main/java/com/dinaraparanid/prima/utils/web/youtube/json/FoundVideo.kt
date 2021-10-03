package com.dinaraparanid.prima.utils.web.youtube.json

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
data class FoundVideo(
    @Expose @JvmField val kind: String,
    @Expose @JvmField val etag: String,
    @Expose @JvmField val id: Id,
    @Expose @JvmField val snippet: Snippet
)