package com.dinaraparanid.prima.utils.web

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HappiApi {
    private companion object {
        private const val KEY = "77d859wJedc17UZ6ByrZJGTZVPp50tFmyMc0DWRUvQ9dM8NDF3CTkaw7"
    }

    @GET("v1/music?")
    fun fetchTrackDataSearch(
        @Query("q") search: String,
        @Query("limit") limit: String = "",
        @Query("apikey") apiKey: String,
        @Query("type") type: String = "lyrics",
        @Query("lyrics") lyrics: Int = 1
    ): Call<String>

    @GET("v1/music/artists/{artist}/albums/{album}/tracks/{track}/lyrics?")
    fun fetchLyrics(
        @Path("artist") artist: String,
        @Path("album") album: String,
        @Path("track") track: String,
        @Query("apikey") apiKey: String,
    ): Call<String>
}