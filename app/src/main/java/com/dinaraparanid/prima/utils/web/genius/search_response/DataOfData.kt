package com.dinaraparanid.prima.utils.web.genius.search_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/** Another info from Genius API */

class DataOfData(
    @Expose
    @SerializedName("highlights")
    val highlights: Array<*>,

    @Expose
    @SerializedName("index")
    val index: String,

    @Expose
    @SerializedName("type")
    val type: String,

    @Expose
    @SerializedName("result")
    val result: Result
)