package com.dinaraparanid.prima.mvvmp.presenters

import com.dinaraparanid.prima.entities.Track
import kotlinx.coroutines.flow.StateFlow

/** [BasePresenter] for track items in TrackListFragments */

class TrackItemPresenter(
    private val trackList: List<Track>,
    private val curItemIndex: Int,
    private val currentPlayingTrackPathFlow: StateFlow<String>
) : BasePresenter() {
    private val track = trackList[curItemIndex]

    @JvmField
    val trackTitle = track.titleFormatted

    @JvmField
    val trackArtistAndAlbum = track.artistAndAlbumFormatted

    @JvmField
    val trackPosition = "${curItemIndex + 1}"

    val textColor
        @JvmName("getTextColor")
        get() = when (trackList[curItemIndex].path) {
            currentPlayingTrackPathFlow.value -> primaryColor
            else -> fontColor
        }

    internal inline val isCoverRotating
        @JvmName("isCoverRotating")
        get() = params.isCoverRotating
}