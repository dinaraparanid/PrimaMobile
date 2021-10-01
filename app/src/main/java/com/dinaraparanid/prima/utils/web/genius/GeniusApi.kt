package com.dinaraparanid.prima.utils.web.genius

import com.dinaraparanid.prima.utils.web.genius.search_response.SearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeniusApi {
    private companion object {
        private const val ACCESS_TOKEN = "..." // Not for broadcast...
    }

    @GET("search?access_token=$ACCESS_TOKEN")
    fun fetchTrackDataSearch(@Query("q") query: String): Call<SearchResponse>
}