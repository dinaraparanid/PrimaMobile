package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivityViewModel : ViewModel() {
    internal val allTracksLiveData = MutableLiveData<Playlist>()
    internal val playingIdLiveData = MutableLiveData<Option<Long>>()
    internal val curPlaylistLiveData = MutableLiveData<Playlist>()
    internal val isPlayingLiveData = MutableLiveData<Boolean>()
    internal val sheetBehaviorPositionLiveData = MutableLiveData<Int>()

    internal var like = false
    internal var repeat1 = false
    internal var actionBarSize = 0
    internal var tracks = mutableListOf<Track>()

    fun load(
        playingId: Long?,
        isPlaying: Boolean?,
        curPlaylist: Playlist?,
        sheetBehaviorPosition: Int?,
        allTracks: Playlist?,
    ) {
        playingIdLiveData.value = playingId?.let { Some(it) } ?: None
        isPlayingLiveData.value = isPlaying ?: false
        curPlaylistLiveData.value = curPlaylist ?: Playlist()
        sheetBehaviorPositionLiveData.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
        allTracksLiveData.value = allTracks ?: Playlist()
    }
}