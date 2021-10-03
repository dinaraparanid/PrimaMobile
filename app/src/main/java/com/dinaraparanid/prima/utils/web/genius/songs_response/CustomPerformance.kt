package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.dinaraparanid.prima.utils.web.genius.Artist
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class CustomPerformance(
    @Expose
    @JvmField
    val label: String,

    @Expose
    @JvmField
    val artists: Array<Artist>
) : Serializable