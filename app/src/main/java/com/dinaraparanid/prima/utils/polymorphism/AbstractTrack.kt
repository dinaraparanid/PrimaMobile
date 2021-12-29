package com.dinaraparanid.prima.utils.polymorphism

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Favourable
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteTrack
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.random.Random

/** Parent of all song entities */
abstract class AbstractTrack(
    @Transient open val androidId: Long,
    @SerializedName("_title")
    @Transient open val title: String,
    @Transient open val artist: String,
    @Transient open val playlist: String,
    @SerializedName("_path")
    @Transient open val path: String,          // DATA from media columns
    @Transient open val duration: Long,
    @Transient open val relativePath: String?, // RELATIVE_PATH from media columns
    @Transient open val displayName: String?,  // DISPLAY_NAME from media columns
    @Transient open val addDate: Long
) : Serializable, Favourable<FavouriteTrack> {
    internal inline val artistAndAlbumFormatted
        get() = "${artist.takeIf { it != "<unknown>" } ?: 
        Params.instance.application.unchecked.resources.getString(R.string.unknown_artist)} / ${playlist.takeIf { 
            it != "<unknown>" && it != path.split('/').takeLast(2).first()
        } ?: Params.instance.application.unchecked.resources.getString(R.string.unknown_album)}"

    internal inline val gtmFormat
        @JvmName("getGTMFormat")
        get() = "$title ($artist/$playlist)"

    internal fun getGTMRandomPlaybackStartPosition(playbackLength: Byte) =
        (duration - playbackLength).toInt().let { if (it <= 0) 0 else Random.nextInt(it) }

    final override fun asFavourite(): FavouriteTrack = FavouriteTrack(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractTrack) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String =
        "Track(androidId=$androidId, title='$title', artist='$artist', playlist='$playlist', path='$path', duration=$duration, relativePath=$relativePath, displayName=$displayName, addDate=$addDate)"
}