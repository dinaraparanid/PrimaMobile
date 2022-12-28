package com.dinaraparanid.prima.mvvmp.old_shit

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.mvvmp.androidx.TrackSelectViewModel
import java.lang.ref.WeakReference

/**
 * MVVM [ViewModel] for
 * [com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment]
 */

class TrackSelectViewModel(
    track: Track,
    viewModel: TrackSelectViewModel
) : TrackItemViewModel(_track = track) {

    private val _viewModel = WeakReference(viewModel)
    private inline val viewModel get() = _viewModel.unchecked

    private var _isChecked = isChecked

    /** Gets track selector button check status */
    internal inline val isChecked
        @JvmName("isChecked")
        get() = track in viewModel.newSetFlow.value

    /** Adds or removes task to add / remove track */
    @JvmName("onTrackSelectorClicked")
    internal fun onTrackSelectorClicked() {
        _isChecked = !_isChecked

        when {
            _isChecked -> viewModel.newSetFlow.value.add(track)
            else -> viewModel.newSetFlow.value.remove(track)
        }
    }
}