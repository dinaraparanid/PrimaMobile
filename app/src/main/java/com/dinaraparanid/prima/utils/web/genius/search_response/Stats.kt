package com.dinaraparanid.prima.utils.web.genius.search_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Statistics for a searched track
 */

class Stats(
    @Expose
    @SerializedName("unreviewed_annotations")
    val unreviewedAnnotations: Int,

    @Expose
    @SerializedName("concurrents")
    val concurrents: Int,

    @Expose
    @SerializedName("hot")
    val isHot: Boolean,

    @Expose
    @SerializedName("pageviews")
    val pageViews: Long
)