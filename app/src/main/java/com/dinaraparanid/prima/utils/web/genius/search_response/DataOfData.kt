package com.dinaraparanid.prima.utils.web.genius.search_response

import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import com.google.gson.annotations.Expose

/** Another info from Genius API */

class DataOfData(
    @Expose @JvmField val highlights: Array<*>,
    @Expose @JvmField val index: String,
    @Expose @JvmField val type: String,
    @Expose @JvmField val result: GeniusTrack
)