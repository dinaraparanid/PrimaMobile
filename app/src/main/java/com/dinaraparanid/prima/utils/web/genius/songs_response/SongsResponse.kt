package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.dinaraparanid.prima.utils.web.genius.Meta
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SongsResponse(
    @Expose @JvmField val meta: Meta,
    @Expose @JvmField val response: Data
)