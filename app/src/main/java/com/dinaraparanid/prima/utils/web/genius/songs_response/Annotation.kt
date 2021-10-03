package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Annotation(
    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    val body: Body,

    @Expose
    @JvmField
    @SerializedName("comment_count")
    val commentCount: Long,

    @Expose
    @JvmField
    @SerializedName("community")
    val hasCommunity: Boolean,

    @Expose
    @JvmField
    @SerializedName("custom_preview")
    val customPreview: String? = null,

    @Expose
    @JvmField
    @SerializedName("has_voters")
    val hasVoters: Boolean,

    @Expose
    @JvmField
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("pinned")
    val isPinned: Boolean,

    @Expose
    @JvmField
    @SerializedName("share_url")
    val shareUrl: String,

    @Expose
    @JvmField
    val source: String? = null,

    @Expose
    @JvmField
    val state: String,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    @SerializedName("verified")
    val isVerified: Boolean,

    @Expose
    @JvmField
    @SerializedName("votes_total")
    val votesTotal: Int,

    @Expose
    @JvmField
    @SerializedName("current_user_metadata")
    val currentUserMetadata: CurrentUserMetadata,

    @Expose
    @JvmField
    val authors: Array<Author>,

    @Expose
    @JvmField
    @SerializedName("cosigned_by")
    val cosignedBy: Array<*>,

    @Expose
    @JvmField
    @SerializedName("rejection_comment")
    val rejectionComment: String? = null,

    @Expose
    @JvmField
    @SerializedName("verified_by")
    val verifiedBy: String? = null
) : Serializable