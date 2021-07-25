package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * [ViewModel] for [com.dinaraparanid.prima.MainActivity]
 */

class MainActivityViewModel : ViewModel() {
    internal val sheetBehaviorPositionLiveData = MutableLiveData<Int>()
    internal val progressLiveData = MutableLiveData<Int>()
    internal val trackSelectedLiveData = MutableLiveData<Boolean>()

    /**
     * Loading params for activity
     * @param sheetBehaviorPosition position of playing panel
     * @param progress time progress
     * @param trackSelected was track selected (to show playing panel)
     * @see [BottomSheetBehavior]
     */

    fun load(
        sheetBehaviorPosition: Int?,
        progress: Int?,
        trackSelected: Boolean?,
    ) {
        sheetBehaviorPositionLiveData.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
        progressLiveData.value = progress ?: -1
        trackSelectedLiveData.value = trackSelected ?: false
    }
}