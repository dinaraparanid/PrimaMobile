package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.extensions.unwrap

/**
 * MVVM View Model for track item
 */

open class TrackItemViewModel(@JvmField internal val num: Int) : ViewModel() {
    /** Formats track title */
    @JvmName("getTitle")
    internal fun getTitle(track: AbstractTrack) = track.title.let {
        if (it == "<unknown>") params.application.unchecked.resources.getString(R.string.unknown_track) else it
    }

    /** Formats track's artist and album */
    @JvmName("getArtistAndAlbum")
    internal fun getArtistAndAlbum(track: AbstractTrack) = track.artistAndAlbumFormatted

    /** Gets track number as string */
    @JvmName("getNumber")
    internal fun getNumber() = num.toString()

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