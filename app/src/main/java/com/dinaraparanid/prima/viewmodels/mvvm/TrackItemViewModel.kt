package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.extensions.unwrap

/**
 * MVVM View Model for track item
 */

open class TrackItemViewModel(@JvmField internal val num: Int) : ViewModel() {
    /** Formats track title */
    @JvmName("getTitle")
    internal fun getTitle(track: Track) = track.title.let {
        if (it == "<unknown>") params.application.resources.getString(R.string.unknown_track) else it
    }

    /** Formats track's artist and album */
    @JvmName("getArtistAndAlbum")
    internal fun getArtistAndAlbum(track: Track) = track.artistAndAlbumFormatted

    /** Gets track number as string */
    @JvmName("getNumber")
    internal fun getNumber() = num.toString()

    /** Gets text color depending on what track is currently playing */
    @JvmName("getTextColor")
    internal fun getTextColor(tracks: Array<Track>, position: Int) = when {
        (params.application as MainApplication).highlightedRow.isEmpty() -> params.fontColor

        tracks[position].path ==
                (params.application as MainApplication).highlightedRow.unwrap() ->
            params.primaryColor

        else -> params.fontColor
    }
}