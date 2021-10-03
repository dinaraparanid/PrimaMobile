package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Interactions(
    @Expose
    @JvmField
    @SerializedName("pyong")
    val isPyonged: Boolean,

    @Expose
    @JvmField
    @SerializedName("following")
    val isFollowing: Boolean
) : Serializable