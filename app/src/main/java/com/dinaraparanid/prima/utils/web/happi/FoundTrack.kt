package com.dinaraparanid.prima.utils.web.happi

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Track from web search
 */

@Deprecated("Switched to Genius API")
data class FoundTrack(
    override val androidId: Long,

    @Expose
    override val title: String,

    @Expose
    override val artist: String,

    @Expose
    override val playlist: String,

    override val path: String,
    override val duration: Long,
    override val relativePath: String?,
    override val displayName: String?,
    override val addDate: Long,

    @Expose
    @JvmField
    @SerializedName("id_track")
    val trackId: Long,

    @Expose
    @JvmField
    @SerializedName("id_artist")
    val artistId: Long,

    @Expose
    @JvmField
    @SerializedName("id_album")
    val albumId: Long,

    @Expose
    @JvmField
    val cover: String,

    @Expose
    @JvmField
    @SerializedName("api_artist")
    val apiArtist: String,

    @Expose
    @JvmField
    @SerializedName("api_albums")
    val apiAlbums: String,

    @Expose
    @JvmField
    @SerializedName("api_album")
    val apiAlbum: String,

    @Expose
    @JvmField
    @SerializedName("api_tracks")
    val apiTracks: String,

    @Expose
    @JvmField
    @SerializedName("api_track")
    val apiTrack: String,

    @Expose
    @JvmField
    @SerializedName("api_lyrics")
    val apiLyrics: String
) : AbstractTrack(androidId, title, artist, playlist, path, duration, relativePath, displayName, addDate)