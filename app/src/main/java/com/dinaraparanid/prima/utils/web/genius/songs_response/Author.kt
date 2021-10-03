package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Author(
    @Expose
    @JvmField
    val attribution: Int,

    @Expose
    @JvmField
    @SerializedName("pinned_role")
    val pinnedRole: String? = null,

    @Expose
    @JvmField
    val user: User
)