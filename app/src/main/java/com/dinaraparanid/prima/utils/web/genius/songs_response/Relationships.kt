package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@JvmInline
value class Relationships(
    @Expose
    @SerializedName("pinned_role")
    val pinnedRole: String?
) : Serializable