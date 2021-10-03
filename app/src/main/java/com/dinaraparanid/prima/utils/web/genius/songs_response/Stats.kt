package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Stats(
    @Expose
    @JvmField
    @SerializedName("accepted_annotations")
    val acceptedAnnotations: Int,

    @Expose
    @JvmField
    val contributors: Int,

    @Expose
    @JvmField
    @SerializedName("iq_earners")
    val iqEarners: Int,

    @Expose
    @JvmField
    val transcribers: Int,

    @Expose
    @JvmField
    @SerializedName("unreviewed_annotations")
    val unreviewedAnnotations: Int,

    @Expose
    @JvmField
    @SerializedName("verified_annotations")
    val verifiedAnnotations: Int,

    @Expose
    @JvmField
    @SerializedName("hot")
    val isHot: Boolean,

    @Expose
    @JvmField
    @SerializedName("pageviews")
    val pageViews: Long
)