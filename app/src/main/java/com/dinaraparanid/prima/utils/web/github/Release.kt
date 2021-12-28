package com.dinaraparanid.prima.utils.web.github

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Release(
    @Expose
    @JvmField
    @SerializedName("url")
    val url: String,

    @Expose
    @JvmField
    @SerializedName("assets_url")
    val assetsUrl: String,

    @Expose
    @JvmField
    @SerializedName("html_url")
    val htmlUrl: String,

    @Expose
    @JvmField
    @SerializedName("id")
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("author")
    val author: Author,

    @Expose
    @JvmField
    @SerializedName("node_id")
    val nodeId: String,

    @Expose
    @JvmField
    @SerializedName("tag_name")
    val tagName: String,

    @Expose
    @JvmField
    @SerializedName("target_commitish")
    val targetCommitish: String,

    @Expose
    @JvmField
    @SerializedName("name")
    val name: String,

    @Expose
    @JvmField
    @SerializedName("draft")
    val isDraft: Boolean,

    @Expose
    @JvmField
    @SerializedName("prerelease")
    val isPrerelease: Boolean,

    @Expose
    @JvmField
    @SerializedName("created_at")
    val createdAt: String,

    @Expose
    @JvmField
    @SerializedName("published_at")
    val publishedAt: String,

    @Expose
    @JvmField
    @SerializedName("assets")
    val assets: List<Any>,

    @Expose
    @JvmField
    @SerializedName("tarball_url")
    val tarballUrl: String,

    @Expose
    @JvmField
    @SerializedName("zipball_url")
    val zipballUrl: String,

    @Expose
    @JvmField
    @SerializedName("body")
    val body: String,
) : Serializable