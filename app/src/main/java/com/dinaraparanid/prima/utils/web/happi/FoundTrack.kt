package com.dinaraparanid.prima.utils.web.happi

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Track from web search
 */

@Deprecated("Switched to Genius API")
data class FoundTrack(
    @JvmField
    override val androidId: Long,

    @Expose
    @JvmField
    override val title: String,

    @Expose
    @JvmField
    override val artist: String,

    @Expose
    @JvmField
    override val playlist: String,

    @JvmField override val path: String,
    @JvmField override val duration: Long,
    @JvmField override val relativePath: String?,
    @JvmField override val displayName: String?,
    @JvmField override val addDate: Long,

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