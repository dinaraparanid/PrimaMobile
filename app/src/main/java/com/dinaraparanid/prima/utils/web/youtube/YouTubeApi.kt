package com.dinaraparanid.prima.utils.web.youtube

import com.dinaraparanid.prima.utils.Params
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
interface YouTubeApi {
    @GET(
        "search?part=snippet&channelType=any&eventType=none&videoDefinition=any&" +
                "videoDimension=any&videoDuration=any&videoEmbeddable=any&videoLicense=any&" +
                "videoSyndicated=any&videoType=any&key=${Params.YOUTUBE_API}"
    )
    fun fetchSearch(
        @Query("q") query: String,
        @Query("order") order: String = "viewCount",
        @Query("maxResults") maxResults: Int = 50
    ): Call<String>
}