package com.dinaraparanid.prima.utils.web

import com.google.gson.annotations.Expose

/**
 * Helps to parse response json string to fetch lyrics
 */

class LyricsParser(
    @Expose val success: Boolean,
    @Expose val length: Int,
    @Expose val result: Lyrics
)