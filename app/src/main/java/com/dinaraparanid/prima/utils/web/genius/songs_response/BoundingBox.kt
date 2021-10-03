package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose

data class BoundingBox(
    @Expose @JvmField val width: Int,
    @Expose @JvmField val height: Int
)
