package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class IqByAction(
    @Expose
    @JvmField
    @SerializedName("edit_metadata")
    val editMetadata: EditMetadata
) : Serializable