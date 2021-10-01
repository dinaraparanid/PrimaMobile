package com.dinaraparanid.prima.utils.web.genius.search_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Found song from search query
 */

@JvmInline
value class Data(
    @Expose
    @SerializedName("hits")
    val hits: Array<DataOfData>
)