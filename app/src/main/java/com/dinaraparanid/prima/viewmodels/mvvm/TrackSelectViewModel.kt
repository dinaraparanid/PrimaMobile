package com.dinaraparanid.prima.viewmodels.mvvm

import android.widget.CheckBox
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectedViewModel
import java.lang.ref.WeakReference

class TrackSelectViewModel(
    track: AbstractTrack,
    viewModel: TrackSelectedViewModel,
    private val tracksSet: Set<String>,
    trackSelector: CheckBox
) : TrackItemViewModel(_track = track) {

    private val _viewModel = WeakReference(viewModel)
    private inline val viewModel get() = _viewModel.unchecked

    private val _trackSelector = WeakReference(trackSelector)
    private inline val trackSelector get() = _trackSelector.unchecked

    /** Gets track selector button check status */
    internal inline val trackSelectorButtonStatus
        @JvmName("getTrackSelectorButtonStatus")
        get() = track !in viewModel.removeSetFlow.value
                && (track in viewModel.addSetFlow.value
                || track.path in tracksSet)

    /** Adds or removes task to add / remove track */
    @JvmName("onTrackSelectorClicked")
    internal fun onTrackSelectorClicked() = when {
        trackSelector.isChecked -> viewModel.addSetFlow.value.add(track)

        else -> when (track) {
            in viewModel.addSetFlow.value ->
                viewModel.addSetFlow.value.remove(track)

            else -> viewModel.removeSetFlow.value.add(track)
        }
    }
}