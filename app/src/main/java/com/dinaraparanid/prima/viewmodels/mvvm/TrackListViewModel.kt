package com.dinaraparanid.prima.viewmodels.mvvm

import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment
import java.lang.ref.WeakReference

/**
 * MVVM View Model for track fragments
 */

open class TrackListViewModel<T, A, VH, B>(f: TrackListSearchFragment<T, A, VH, B>) :
    ViewModel()
        where T : AbstractTrack,
              VH : RecyclerView.ViewHolder,
              A : RecyclerView.Adapter<VH>,
              B : ViewDataBinding {

    private val fragment = WeakReference(f)

    /** Sorts fragment's tracks in selected order*/
    @JvmName("onTrackOrderButtonPressed")
    internal fun onTrackOrderButtonPressed(view: View) = fragment.unchecked.onTrackOrderButtonPressed(view)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() = fragment.unchecked.onShuffleButtonPressed()
}