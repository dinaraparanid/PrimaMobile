package com.dinaraparanid.prima.utils.web.happi

import com.dinaraparanid.prima.core.Track
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Track from web search
 */

class FoundTrack(
    override val androidId: Long,

    @Expose
    @SerializedName("track")
    override val title: String,

    @Expose
    override val artist: String,

    @Expose
    @SerializedName("album")
    override val playlist: String,

    override val path: String,
    override val duration: Long,
    override val relativePath: String?,
    override val displayName: String?,
    override val addDate: Long,

    @Expose
    @SerializedName("id_track")
    val trackId: Long,

    @Expose
    @SerializedName("id_artist")
    val artistId: Long,

    @Expose
    @SerializedName("id_album")
    val albumId: Long,

    @Expose
    val cover: String,

    @Expose
    @SerializedName("api_artist")
    val apiArtist: String,

    @Expose
    @SerializedName("api_albums")
    val apiAlbums: String,

    @Expose
    @SerializedName("api_album")
    val apiAlbum: String,

    @Expose
    @SerializedName("api_tracks")
    val apiTracks: String,

    @Expose
    @SerializedName("api_track")
    val apiTrack: String,

    @Expose
    @SerializedName("api_lyrics")
    val apiLyrics: String
) : Track(androidId, title, artist, playlist, path, duration, relativePath, displayName, addDate)