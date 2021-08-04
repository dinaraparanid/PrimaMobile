package com.dinaraparanid.prima.utils.web

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Presentation of response lyrics json string
 */

class Lyrics(
    @Expose
    val artist: String,

    @Expose
    @SerializedName("id_artist")
    val idArtist: Long,

    @Expose
    val track: String,

    @Expose
    @SerializedName("id_track")
    val idTrack: Long,

    @Expose
    @SerializedName("id_album")
    val idAlbum: Long,

    @Expose
    val album: String,

    @Expose
    val lyrics: String
)