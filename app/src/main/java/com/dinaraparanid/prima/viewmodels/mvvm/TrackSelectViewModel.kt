package com.dinaraparanid.prima.viewmodels.mvvm

import android.widget.CheckBox
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectedViewModel

class TrackSelectViewModel(
    number: Int,
    private val track: Track,
    private val viewModel: TrackSelectedViewModel,
    private val tracksSet: Set<String>,
    private val trackSelector: CheckBox
) : TrackItemViewModel(number) {

    /** Gets track selector button check status */
    @JvmName("getTrackSelectorButtonStatus")
    internal fun getTrackSelectorButtonStatus() = track !in viewModel.removeSetLiveData.value!!
            && (track in viewModel.addSetLiveData.value!!
            || track.path in tracksSet)

    /** Adds or removes task to add / remove track */
    @JvmName("onTrackSelectorClicked")
    internal fun onTrackSelectorClicked() = when {
        trackSelector.isChecked -> viewModel.addSetLiveData.value!!.add(track)

        else -> when (track) {
            in viewModel.addSetLiveData.value!! ->
                viewModel.addSetLiveData.value!!.remove(track)

            else -> viewModel.removeSetLiveData.value!!.add(track)
        }
    }
}