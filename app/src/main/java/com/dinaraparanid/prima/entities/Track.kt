package com.dinaraparanid.prima.entities

import android.os.Parcel
import android.os.Parcelable
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.Entity
import com.dinaraparanid.prima.databases.entities.favourites.AsFavouriteEntity
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import kotlin.random.Random

/** Parent of all song entities */

open class Track(
    /** _ID from media columns */
    @JvmField val androidId: Long,

    /** TITLE from media columns */
    @JvmField val title: String,

    /** ARTIST from media columns */
    @JvmField val artist: String,

    /** ALBUM from media columns */
    @JvmField val album: String,

    /** DATA from media columns */
    @JvmField val path: String,

    /** DURATION from media columns */
    @JvmField val duration: Long,

    /** RELATIVE_PATH from media columns */
    @JvmField val relativePath: String?,

    /** DISPLAY_NAME from media columns */
    @JvmField val displayName: String?,

    /** DATE_ADDED from media columns */
    @JvmField val addDate: Long,

    /** TRACK from media columns */
    @JvmField val trackNumberInAlbum: Byte,
) : Entity, AsFavouriteEntity<FavouriteTrack>, Parcelable, KoinComponent {
    companion object CREATOR : Parcelable.Creator<Track> {
        const val UNKNOWN_TRACK = "unknown_track"
        const val UNKNOWN_ARTIST = "unknown_artist"
        const val UNKNOWN_ALBUM = "unknown_album"

        override fun createFromParcel(parcel: Parcel): Track {
            return Track(parcel)
        }

        override fun newArray(size: Int): Array<Track?> {
            return arrayOfNulls(size)
        }
    }

    @JvmField
    val titleFormatted = title.let {
        when (it) {
            "<unknown>" -> get(named(UNKNOWN_TRACK))
            else -> it
        }
    }

    @JvmField
    val artistAndAlbumFormatted = "${
        artist.takeIf { it != "<unknown>" } ?: get(named(UNKNOWN_ARTIST))
    } / ${
        album.takeIf { it != "<unknown>" } ?: get(named(UNKNOWN_ALBUM))
    }"

    @JvmField
    val gtmFormat = "$title ($artist/$album)"

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readByte()
    )

    fun getGTMRandomPlaybackStartPosition(playbackLength: Byte) =
        (duration - playbackLength).toInt().let { if (it <= 0) 0 else Random.nextInt(it) }

    final override fun asFavourite() = FavouriteTrack(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        return path == other.path
    }

    override fun hashCode() = path.hashCode()

    override fun toString() =
        "Track(androidId=$androidId, title='$title', artist='$artist', playlist='$album', path='$path', duration=$duration, relativePath=$relativePath, displayName=$displayName, addDate=$addDate)"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(androidId)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(album)
        parcel.writeString(path)
        parcel.writeLong(duration)
        parcel.writeString(relativePath)
        parcel.writeString(displayName)
        parcel.writeLong(addDate)
        parcel.writeByte(trackNumberInAlbum)
    }

    final override fun describeContents() = 0
}