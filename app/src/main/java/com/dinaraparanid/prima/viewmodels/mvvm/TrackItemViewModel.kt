package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track

/**
 * MVVM View Model for track item
 */

open class TrackItemViewModel : ViewModel() {
    /** Formats track title */
    @JvmName("getTitle")
    internal fun getTitle(track: Track) = track.title.let {
        if (it == "<unknown>") params.application.resources.getString(R.string.unknown_track) else it
    }

    /** Formats track's artist and album */
    @JvmName("getArtistAndAlbum")
    internal fun getArtistAndAlbum(track: Track) = "${
        track.artist
            .let {
                when (it) {
                    "<unknown>" -> params.application.resources.getString(R.string.unknown_artist)
                    else -> it
                }
            }
    } / ${track.playlist}"
}