package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.fragments.TrackSelectFragment
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.CustomPlaylistTrackListFragment]
 */

class CustomPlaylistTrackListViewModel(
    fragment: AbstractTrackListFragment<FragmentCustomPlaylistTrackListBinding>,
    act: MainActivity,
    private val mainLabelCurText: String,
    private val playlistId: Long,
    private val itemList: MutableList<AbstractTrack>
) : PlaylistTrackListViewModel<FragmentCustomPlaylistTrackListBinding>(fragment, act) {

    private val activity = WeakReference(act)

    /** shows [com.dinaraparanid.prima.fragments.TrackSelectFragment] */
    @JvmName("onAddTrackButtonClicked")
    internal fun onAddTrackButtonClicked() {
        activity.unchecked.supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrackSelectFragment.newInstance(
                    mainLabelCurText,
                    activity.unchecked.resources.getString(R.string.tracks),
                    playlistId,
                    itemList.toPlaylist()
                )
            )
            .addToBackStack(null)
            .apply {
                activity.unchecked.sheetBehavior.state =
                    BottomSheetBehavior.STATE_COLLAPSED
            }
            .commit()
    }
}