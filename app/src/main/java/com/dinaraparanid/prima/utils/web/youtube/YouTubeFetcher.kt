package com.dinaraparanid.prima.utils.web.youtube

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

@Deprecated(
    "The YouTube API key is very limited in resources, " +
            "and it will not be enough for users from the Play Market"
)
class YouTubeFetcher {
    private val youTubeApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(YouTubeApi::class.java)

    internal fun fetchSearch(
        query: String,
        order: SearchOrder = SearchOrder.ViewCount()
    ): LiveData<String> {
        val responseLiveData = MutableLiveData<String>()

        youTubeApi
            .fetchSearch(query, order.value)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    responseLiveData.value = response.body()
                }

                override fun onFailure(call: Call<String>, t: Throwable) = Unit
            })

        return responseLiveData
    }
}