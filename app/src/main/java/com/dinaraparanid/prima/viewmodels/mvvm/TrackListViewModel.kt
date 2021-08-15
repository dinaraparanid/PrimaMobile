package com.dinaraparanid.prima.viewmodels.mvvm

import android.view.View
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment

/**
 * MVVM View Model for track fragments
 */

open class TrackListViewModel(private val fragment: AbstractTrackListFragment) : ViewModel() {

    /** Sorts fragment's tracks in selected order*/
    @JvmName("onTrackOrderButtonPressed")
    internal fun onTrackOrderButtonPressed(view: View) = fragment.onTrackOrderButtonPressed(view)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() = fragment.onShuffleButtonPressed()
}