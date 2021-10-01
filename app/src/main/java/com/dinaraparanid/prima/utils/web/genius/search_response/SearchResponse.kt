package com.dinaraparanid.prima.utils.web.genius.search_response

import com.dinaraparanid.prima.utils.web.genius.Meta
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Search response from Genius API
 * on search query
 */

class SearchResponse(
    @Expose
    @SerializedName("meta")
    val meta: Meta,

    @Expose
    @SerializedName("response")
    val response: Data
)