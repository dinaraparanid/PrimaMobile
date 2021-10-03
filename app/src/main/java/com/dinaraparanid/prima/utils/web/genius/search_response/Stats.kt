package com.dinaraparanid.prima.utils.web.genius.search_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Statistics for a searched track
 */

data class Stats(
    @Expose
    @JvmField
    @SerializedName("unreviewed_annotations")
    val unreviewedAnnotations: Int,

    @Expose
    @JvmField
    val concurrents: Int,

    @Expose
    @JvmField
    @SerializedName("hot")
    val isHot: Boolean,

    @Expose
    @JvmField
    @SerializedName("pageviews")
    val pageViews: Long
) : Serializable