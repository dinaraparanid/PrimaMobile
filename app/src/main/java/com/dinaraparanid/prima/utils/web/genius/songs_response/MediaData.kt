package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose

data class MediaData(
    @Expose
    @JvmField
    val provider: String,

    @Expose
    @JvmField
    val start: Int,

    @Expose
    @JvmField
    val type: String,

    @Expose
    @JvmField
    val url: String
)