package com.dinaraparanid.prima.viewmodels.mvvm

import android.view.View
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment

/**
 * MVVM View Model for track fragments
 */

open class TrackListViewModel<T, VH>(private val fragment: TrackListSearchFragment<T, VH>) :
    ViewModel()
        where T : Track,
              VH : androidx.recyclerview.widget.RecyclerView.ViewHolder {

    /** Sorts fragment's tracks in selected order*/
    @JvmName("onTrackOrderButtonPressed")
    internal fun onTrackOrderButtonPressed(view: View) = fragment.onTrackOrderButtonPressed(view)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() = fragment.onShuffleButtonPressed()
}