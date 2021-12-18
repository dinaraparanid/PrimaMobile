package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.extensions.unwrap

/** MVVM View Model for track item */

open class TrackItemViewModel(
    @JvmField internal val num: Int
) : ViewModel() {
    internal lateinit var track: AbstractTrack

    internal constructor(num: Int, track: AbstractTrack) : this(num) {
        this.track = track
    }

    /** Formats track title */
    internal inline val title
        @JvmName("getTitle")
        get() = track.title.let {
            when (it) {
                "<unknown>" -> params
                    .application
                    .unchecked
                    .resources
                    .getString(R.string.unknown_track)

                else -> it
            }
        }

    /** Formats track's artist and album */
    internal inline val artistAndAlbum
        @JvmName("getArtistAndAlbum")
        get() = track.artistAndAlbumFormatted

    /** Gets track number as string */
    internal inline val number
        @JvmName("getNumber")
        get() = num.toString()

    /** Gets text color depending on what track is currently playing */
    @JvmName("getTextColor")
    internal fun getTextColor(tracks: Array<AbstractTrack>, position: Int) = when {
        (params.application.unchecked as MainApplication).highlightedRow.isEmpty() -> params.fontColor

        tracks[position].path ==
                (params.application.unchecked as MainApplication).highlightedRow.unwrap() ->
            params.primaryColor

        else -> params.fontColor
    }
}