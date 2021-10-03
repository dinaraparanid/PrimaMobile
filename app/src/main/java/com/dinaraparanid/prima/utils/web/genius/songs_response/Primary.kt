package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Primary(
    @Expose
    @JvmField
    val multiplier: Int,

    @Expose
    @JvmField
    val base: Int,

    @Expose
    @JvmField
    @SerializedName("applicable")
    val isApplicable: Boolean
)