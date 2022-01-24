package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.fragments.track_lists.CustomPlaylistTrackListFragment
import com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.google.android.material.bottomsheet.BottomSheetBehavior

/** MVVM View Model for [CustomPlaylistTrackListFragment] */

class CustomPlaylistTrackListViewModel(
    playlistTitle: String,
    fragment: CustomPlaylistTrackListFragment,
    private val playlistId: Long,
    private val itemList: List<AbstractTrack>
) : PlaylistTrackListViewModel<FragmentCustomPlaylistTrackListBinding, CustomPlaylistTrackListFragment>(
    playlistTitle,
    AbstractPlaylist.PlaylistType.CUSTOM.ordinal,
    fragment
) {

    /** shows [com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment] */
    @JvmName("onAddTrackButtonClicked")
    internal fun onAddTrackButtonClicked() {
        fragment.unchecked.requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrackSelectFragment.Builder(
                    TrackSelectFragment.Companion.TracksSelectionTarget.CUSTOM,
                    *itemList.toTypedArray()
                ).setPlaylistId(playlistId).build()
            )
            .addToBackStack(null)
            .apply {
                (fragment.unchecked.requireActivity() as MainActivity).sheetBehavior.state =
                    BottomSheetBehavior.STATE_COLLAPSED
            }
            .commit()
    }
}