package com.dinaraparanid.prima.viewmodels.mvvm

import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment

/**
 * MVVM View Model for track fragments
 */

open class TrackListViewModel<T, A, VH, B>(private val fragment: TrackListSearchFragment<T, A, VH, B>) :
    ViewModel()
        where T : Track,
              VH : RecyclerView.ViewHolder,
              A : RecyclerView.Adapter<VH>,
              B : ViewDataBinding {

    /** Sorts fragment's tracks in selected order*/
    @JvmName("onTrackOrderButtonPressed")
    internal fun onTrackOrderButtonPressed(view: View) = fragment.onTrackOrderButtonPressed(view)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() = fragment.onShuffleButtonPressed()
}