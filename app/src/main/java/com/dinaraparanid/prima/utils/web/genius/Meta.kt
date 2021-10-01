package com.dinaraparanid.prima.utils.web.genius

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@JvmInline
value class Meta(
    @Expose
    @SerializedName("status")
    val status: Short
)