package com.dinaraparanid.prima.utils.web.happi

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Presentation of response lyrics json string
 */

@Deprecated("Switched to Genius API")
data class Lyrics(
    @Expose
    @JvmField
    val artist: String,

    @Expose
    @JvmField
    @SerializedName("id_artist")
    val idArtist: Long,

    @Expose
    @JvmField
    val track: String,

    @Expose
    @JvmField
    @SerializedName("id_track")
    val idTrack: Long,

    @Expose
    @JvmField
    @SerializedName("id_album")
    val idAlbum: Long,

    @Expose
    @JvmField
    val album: String,

    @Expose
    @JvmField
    val lyrics: String
)