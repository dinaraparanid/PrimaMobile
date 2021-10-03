package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import java.io.Serializable

@JvmInline
value class Body(@Expose @JvmField val plain: String) : Serializable