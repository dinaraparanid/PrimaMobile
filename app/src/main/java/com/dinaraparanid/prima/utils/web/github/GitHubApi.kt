package com.dinaraparanid.prima.utils.web.github

import retrofit2.Call
import retrofit2.http.GET

interface GitHubApi {
    @GET("https://api.github.com/repos/dinaraparanid/PrimaMobile/releases")
    fun fetchLatestRelease(): Call<Array<ReleaseInfo>>
}