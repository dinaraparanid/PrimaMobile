package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.core.Track
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivityViewModel : ViewModel() {
    internal val sheetBehaviorPositionLiveData = MutableLiveData<Int>()
    internal val repeat1LiveData = MutableLiveData<Boolean>()
    internal val progressLiveData = MutableLiveData<Int>()
    internal val curIndexLiveData = MutableLiveData<Int>()
    internal val trackSelectedLiveData = MutableLiveData<Boolean>()

    fun load(
        sheetBehaviorPosition: Int?,
        repeat1: Boolean?,
        progress: Int?,
        curIndex: Int?,
        trackSelected: Boolean?,
    ) {
        sheetBehaviorPositionLiveData.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
        repeat1LiveData.value = repeat1 ?: false
        progressLiveData.value = progress ?: 0
        curIndexLiveData.value = curIndex ?: -1
        trackSelectedLiveData.value = trackSelected ?: false
    }
}