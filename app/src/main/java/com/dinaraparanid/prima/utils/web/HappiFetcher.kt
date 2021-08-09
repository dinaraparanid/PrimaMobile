package com.dinaraparanid.prima.utils.web

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.annotations.Expose
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Searches for track info:
 * 1. Makes GET call to found all matches of track
 * 2. Makes GET call to fetch track's lyrics
 */

class HappiFetcher {
    private val happiApi: HappiApi

    internal class ParseObject(
        @Expose val success: Boolean,
        @Expose private val length: Int,
        @Expose val result: Array<FoundTrack>
    )

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.happi.dev/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        happiApi = retrofit.create(HappiApi::class.java)
    }

    private fun mFetchTrackDataSearch(funcCal: Call<String>): LiveData<String> {
        val responseLiveData = MutableLiveData<String>()

        funcCal.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                responseLiveData.value = response.body()
            }

            override fun onFailure(call: Call<String>, t: Throwable) = Unit
        })

        return responseLiveData
    }

    /**
     * Fetches found tracks from search
     * @param search text to search
     * @return json string with found tracks' data
     */

    fun fetchTrackDataSearch(search: String, apiKey: String): LiveData<String> =
        mFetchTrackDataSearch(happiApi.fetchTrackDataSearch(search, apiKey = apiKey))

    /**
     * Fetches found tracks with lyrics from search
     * @param search text to search
     * @return json string with found tracks' data
     */

    fun fetchTrackDataSearchWithLyrics(search: String, apiKey: String): LiveData<String> =
        mFetchTrackDataSearch(happiApi.fetchTrackDataSearchWithLyrics(search, apiKey = apiKey))

    /**
     * Fetches lyrics of searched track
     * @param track track which lyrics is searching
     * @return json string with lyrics
     */

    fun fetchLyrics(track: FoundTrack, apiKey: String): LiveData<String> {
        val responseLiveData = MutableLiveData<String>()

        val lyricsRequest = happiApi.fetchLyrics(
            track.artistId.toString(),
            track.albumId.toString(),
            track.trackId.toString(),
            apiKey
        )

        lyricsRequest.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                responseLiveData.value = response.body()
            }

            override fun onFailure(call: Call<String>, t: Throwable) = Unit
        })

        return responseLiveData
    }
}