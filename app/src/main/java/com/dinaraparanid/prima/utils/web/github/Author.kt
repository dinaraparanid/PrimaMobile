package com.dinaraparanid.prima.utils.web.github

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Author(
    @Expose
    @JvmField
    @SerializedName("login")
    val login: String,

    @Expose
    @JvmField
    @SerializedName("id")
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("node_url")
    val nodeId: String,

    @Expose
    @JvmField
    @SerializedName("avatar_url")
    val avatarUrl: String,

    @Expose
    @JvmField
    @SerializedName("gravatar_id")
    val gravatarId: String,

    @Expose
    @JvmField
    @SerializedName("url")
    val url: String,

    @Expose
    @JvmField
    @SerializedName("html_url")
    val htmlUrl: String,

    @Expose
    @JvmField
    @SerializedName("followers_url")
    val followersUrl: String,

    @Expose
    @JvmField
    @SerializedName("following_url")
    val followingUrl: String,

    @Expose
    @JvmField
    @SerializedName("gists_url")
    val gistsUrl: String,

    @Expose
    @JvmField
    @SerializedName("starred_url")
    val starred_url: String,

    @Expose
    @JvmField
    @SerializedName("subscriptions_url")
    val subscriptionsUrl: String,

    @Expose
    @JvmField
    @SerializedName("organisations_url")
    val organisationsUrl: String,

    @Expose
    @JvmField
    @SerializedName("repos_url")
    val reposUrl: String,

    @Expose
    @JvmField
    @SerializedName("events_url")
    val eventsUrl: String,

    @Expose
    @JvmField
    @SerializedName("received_events_url")
    val receivedEventsUrl: String,

    @Expose
    @JvmField
    @SerializedName("type")
    val type: String,

    @Expose
    @JvmField
    @SerializedName("site_admin")
    val isSiteAdmin: Boolean
) : Serializable
