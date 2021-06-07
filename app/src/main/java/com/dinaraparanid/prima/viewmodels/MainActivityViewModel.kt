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
    internal val isPlayingLiveData = MutableLiveData<Boolean>()
    internal val sheetBehaviorPositionLiveData = MutableLiveData<Int>()
    internal val repeat1LiveData = MutableLiveData<Boolean>()
    internal val progressLiveData = MutableLiveData<Int>()

    internal var like = false
    internal var actionBarSize = 0

    fun load(
        isPlaying: Boolean?,
        sheetBehaviorPosition: Int?,
        repeat1: Boolean?,
        progress: Int?
    ) {
        isPlayingLiveData.value = isPlaying ?: false
        sheetBehaviorPositionLiveData.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
        repeat1LiveData.value = repeat1 ?: false
        progressLiveData.value = progress ?: 0
    }
}