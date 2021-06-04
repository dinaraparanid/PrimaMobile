package com.dinaraparanid.prima.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.MusicRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.UUID

class MainActivityViewModel : ViewModel() {
    private val musicRepository = MusicRepository.getInstance()
    internal val playingIdLiveData = MutableLiveData<UUID?>()
    internal val curPlaylistLiveData = MutableLiveData<Playlist>()
    internal val isPlayingLiveData = MutableLiveData<Boolean>()
    internal val sheetBehaviorPositionLiveData = MutableLiveData<Int>()

    internal var like = false
    internal var repeat1 = false
    internal var actionBarSize = 0
    internal var tracks = mutableListOf<Track>()

    fun load(
        playingId: UUID?,
        isPlaying: Boolean?,
        curPlaylist: Playlist?,
        sheetBehaviorPosition: Int?
    ) {
        playingIdLiveData.value = playingId
        isPlayingLiveData.value = isPlaying ?: false
        curPlaylistLiveData.value = curPlaylist ?: Playlist()
        sheetBehaviorPositionLiveData.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
    }

    fun saveTrack(track: Track): Unit = musicRepository.updateTrack(track)
}