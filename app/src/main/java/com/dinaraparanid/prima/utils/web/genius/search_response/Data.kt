package com.dinaraparanid.prima.utils.web.genius.search_response

import com.google.gson.annotations.Expose

/**
 * Found song from search query
 */

@JvmInline
value class Data(@Expose @JvmField val hits: Array<DataOfData>)