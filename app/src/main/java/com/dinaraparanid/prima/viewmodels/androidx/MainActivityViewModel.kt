package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [ViewModel] for [com.dinaraparanid.prima.MainActivity]
 */

class MainActivityViewModel : ViewModel() {
    internal val sheetBehaviorPositionFlow = MutableStateFlow(BottomSheetBehavior.STATE_COLLAPSED)
    internal val progressFlow = MutableStateFlow(-1)
    internal val trackSelectedFlow = MutableStateFlow(false)

    /**
     * Loading params for an activity
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
        sheetBehaviorPositionFlow.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
        progressFlow.value = progress ?: -1
        trackSelectedFlow.value = trackSelected ?: false
    }
}