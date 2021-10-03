package com.dinaraparanid.prima.utils.web.genius

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Meta(
    @Expose
    @SerializedName("status")
    val status: Short,

    @Expose
    @SerializedName("message")
    val message: String? = null
)