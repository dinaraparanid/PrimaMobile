package com.dinaraparanid.prima.utils.web.genius.search_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Track's data itself
 */

class Result(
    @Expose
    @SerializedName("annotation_count")
    val annotationCount: Int,

    @Expose
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @SerializedName("full_title")
    val fullTitle: String,

    @Expose
    @SerializedName("header_image_thumbnail_url")
    val headerImageThumbnailUrl: String,

    @Expose
    @SerializedName("header_image_url")
    val headerImageUrl: String,

    @Expose
    @SerializedName("id")
    val id: Long,

    @Expose
    @SerializedName("lyrics_owner_id")
    val lyricsOwnerId: Long,

    @Expose
    @SerializedName("lyrics_state")
    val lyricsState: String,

    @Expose
    @SerializedName("path")
    val pathToLyrics: String,

    @Expose
    @SerializedName("pyongs_count")
    val pyongsCount: Long,

    @Expose
    @SerializedName("song_art_image_thumbnail_url")
    val songArtImageThumbnailUrl: String,

    @Expose
    @SerializedName("song_art_image_url")
    val songArtImageUrl: String,

    @Expose
    @SerializedName("stats")
    val stats: Stats,

    @Expose
    @SerializedName("title")
    val title: String,

    @Expose
    @SerializedName("title_with_featured")
    val titleWithFeatured: String,

    @Expose
    @SerializedName("url")
    val url: String,

    @Expose
    @SerializedName("primary_artist")
    val primaryArtist: PrimaryArtist
)