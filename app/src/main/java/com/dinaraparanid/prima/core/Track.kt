package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.utils.Params
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/** Entity for songs */
open class Track(
    open val androidId: Long,
    @SerializedName("_title")
    open val title: String,
    open val artist: String,
    open val playlist: String,
    @SerializedName("_path")
    open val path: String,          // DATA from media columns
    open val duration: Long,
    open val relativePath: String?, // RELATIVE_PATH from media columns
    open val displayName: String?,  // DISPLAY_NAME from media columns
    open val addDate: Long
) : Serializable, Favourable<FavouriteTrack> {
    internal inline val artistAndAlbumFormatted
        get() = "${artist.takeIf { it != "<unknown>" } ?: 
        Params.instance.application.resources.getString(R.string.unknown_artist)} / ${playlist.takeIf { 
            it != "<unknown>" && it != path.split('/').takeLast(2).first()
        } ?: Params.instance.application.resources.getString(R.string.unknown_album)}"

    override fun asFavourite(): FavouriteTrack = FavouriteTrack(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String =
        "Track(androidId=$androidId, title='$title', artist='$artist', playlist='$playlist', path='$path', duration=$duration, relativePath=$relativePath, displayName=$displayName, addDate=$addDate)"
}