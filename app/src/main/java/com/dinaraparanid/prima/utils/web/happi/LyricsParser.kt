package com.dinaraparanid.prima.utils.web.happi

import com.google.gson.annotations.Expose

/**
 * Helps to parse response json string to fetch lyrics
 */

@Deprecated("Switched to Genius API")
data class LyricsParser(
    @Expose @JvmField val success: Boolean,
    @Expose @JvmField val length: Int,
    @Expose @JvmField val result: Lyrics
)