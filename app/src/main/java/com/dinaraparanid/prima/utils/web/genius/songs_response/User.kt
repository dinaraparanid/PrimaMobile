package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class User(
    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    val avatar: Avatar,

    @Expose
    @JvmField
    @SerializedName("header_image_url")
    val headerImageUrl: String,

    @Expose
    @JvmField
    @SerializedName("human_readable_role_for_display")
    val humanReadableRoleForDisplay: String,

    @Expose
    @JvmField
    val id: Long,

    @Expose
    @JvmField
    val iq: Int,

    @Expose
    @JvmField
    val login: String,

    @Expose
    @JvmField
    val name: String,

    @Expose
    @JvmField
    @SerializedName("role_for_display")
    val roleForDisplay: String,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    @SerializedName("current_user_metadata")
    val currentUserMetadata: CurrentUserMetadata
)