package com.dinaraparanid.prima.utils.web.genius

import com.dinaraparanid.prima.utils.web.genius.search_response.SearchResponse
import com.dinaraparanid.prima.utils.web.genius.songs_response.SongsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GeniusApi {
    private companion object {
        private const val ACCESS_TOKEN = "J16avXcn6Yd80xNfzbWmfPz1ji-o5HmyWaDa8zfz269jzgSKhI_M4ZElPZEn5m70" // Not for broadcast...
    }

    @GET("search?access_token=$ACCESS_TOKEN")
    fun fetchTrackDataSearch(@Query("q") query: String): Call<SearchResponse>

    @GET("songs/{id}?access_token=$ACCESS_TOKEN&text_format=plain")
    fun fetchTrackInfoSearch(@Path("id", encoded = true) id: Long): Call<SongsResponse>
}
