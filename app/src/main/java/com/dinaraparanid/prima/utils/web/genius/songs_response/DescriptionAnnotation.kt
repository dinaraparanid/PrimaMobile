package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class DescriptionAnnotation(
    @Expose
    @JvmField
    @SerializedName("_type")
    val type: String,

    @Expose
    @JvmField
    @SerializedName("annotator_id")
    val annotatorId: Long,

    @Expose
    @JvmField
    @SerializedName("annotator_login")
    val annotatorLogin: String,

    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    val classification: String,

    @Expose
    @JvmField
    val fragment: String,

    @Expose
    @JvmField
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("is_description")
    val isDescription: Boolean,

    @Expose
    @JvmField
    val path: String,

    @Expose
    @JvmField
    val range: Range,

    @Expose
    @JvmField
    @SerializedName("song_id")
    val songId: Long,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    @SerializedName("verified_annotator_ids")
    val verifiedAnnotatorIds: Array<*>,

    @Expose
    @JvmField
    val annotatable: Annotatable,

    @Expose
    @JvmField
    val annotations: Array<Annotation>
) : Serializable