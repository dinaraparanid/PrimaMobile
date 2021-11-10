package com.dinaraparanid.prima.viewmodels.mvvm

import android.widget.CheckBox
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectedViewModel

class TrackSelectViewModel(
    number: Int,
    private val track: AbstractTrack,
    private val viewModel: TrackSelectedViewModel,
    private val tracksSet: Set<String>,
    private val trackSelector: CheckBox
) : TrackItemViewModel(number) {

    /** Gets track selector button check status */
    @JvmName("getTrackSelectorButtonStatus")
    internal fun getTrackSelectorButtonStatus() = track !in viewModel.removeSetFlow.value!!
            && (track in viewModel.addSetFlow.value!!
            || track.path in tracksSet)

    /** Adds or removes task to add / remove track */
    @JvmName("onTrackSelectorClicked")
    internal fun onTrackSelectorClicked() = when {
        trackSelector.isChecked -> viewModel.addSetFlow.value!!.add(track)

        else -> when (track) {
            in viewModel.addSetFlow.value!! ->
                viewModel.addSetFlow.value!!.remove(track)

            else -> viewModel.removeSetFlow.value!!.add(track)
        }
    }
}