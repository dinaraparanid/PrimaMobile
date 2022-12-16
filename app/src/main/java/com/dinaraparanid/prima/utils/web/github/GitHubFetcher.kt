package com.dinaraparanid.prima.utils.web.github

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * GitHub API Fetcher.
 * Makes GET call for the latest release of Prima with all data
 */

class GitHubFetcher {
    private val githubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubApi::class.java)

    /**
     * Gets the latest release
     * @return the latest release
     */

    internal fun fetchLatestRelease(): LiveData<ReleaseInfo> {
        val responseLiveData = MutableLiveData<ReleaseInfo>()

        githubApi.fetchLatestRelease().enqueue(object : Callback<Array<ReleaseInfo>> {
            override fun onResponse(
                call: Call<Array<ReleaseInfo>>,
                response: Response<Array<ReleaseInfo>>
            ) {
                responseLiveData.value = response.body()?.first()
            }

            override fun onFailure(call: Call<Array<ReleaseInfo>>, t: Throwable) = Unit
        })

        return responseLiveData
    }
}