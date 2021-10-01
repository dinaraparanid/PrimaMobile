package com.dinaraparanid.prima.utils.web.happi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Deprecated("Switched to Genius API")
interface HappiApi {
    @GET("music?")
    fun fetchTrackDataSearch(
        @Query("q") search: String,
        @Query("limit") limit: String = "50",
        @Query("apikey") apiKey: String,
        @Query("type") type: String = "track, artist",
        @Query("lyrics") lyrics: Int = 0
    ): Call<String>

    @GET("music?")
    fun fetchTrackDataSearchWithLyrics(
        @Query("q") search: String,
        @Query("limit") limit: String = "50",
        @Query("apikey") apiKey: String,
        @Query("type") type: String = "track, artist",
        @Query("lyrics") lyrics: Int = 1
    ): Call<String>

    @GET("music/artists/{artist}/albums/{album}/tracks/{track}/lyrics?")
    fun fetchLyrics(
        @Path("artist") artist: String,
        @Path("album") album: String,
        @Path("track") track: String,
        @Query("apikey") apiKey: String,
    ): Call<String>
}