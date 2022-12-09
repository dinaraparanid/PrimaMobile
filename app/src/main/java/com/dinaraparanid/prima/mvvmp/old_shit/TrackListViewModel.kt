package com.dinaraparanid.prima.mvvmp.old_shit

import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.fragments.TrackListSearchFragment
import java.lang.ref.WeakReference

/** MVVM View Model for track fragments */

open class TrackListViewModel<A, VH, B, F>(f: F) :
    BasePresenter()
        where VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<Pair<Int, AbstractTrack>, VH>,
              B : ViewDataBinding,
              F : TrackListSearchFragment<AbstractTrack, A, VH, B> {

    protected val fragment: WeakReference<F> = WeakReference(f)

    /** Sorts fragment's tracks in selected order*/
    @JvmName("onTrackOrderButtonPressed")
    internal fun onTrackOrderButtonPressed(view: View) = fragment.unchecked.onTrackOrderButtonPressed(view)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() = fragment.unchecked.onShuffleButtonPressed()
}