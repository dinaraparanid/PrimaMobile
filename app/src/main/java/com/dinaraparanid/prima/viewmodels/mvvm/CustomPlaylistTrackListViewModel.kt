package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.fragments.TrackSelectFragment
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.CustomPlaylistTrackListFragment]
 */

class CustomPlaylistTrackListViewModel(
    fragment: AbstractTrackListFragment,
    private val activity: MainActivity,
    private val mainLabelCurText: String,
    private val playlistId: Long,
    private val itemList: MutableList<Track>
) : PlaylistTrackListViewModel(fragment, activity) {

    /** shows [com.dinaraparanid.prima.fragments.TrackSelectFragment] */
    @JvmName("onAddTrackButtonClicked")
    internal fun onAddTrackButtonClicked() {
        activity.supportFragmentManager
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
                    activity.resources.getString(R.string.tracks),
                    playlistId,
                    itemList.toPlaylist()
                )
            )
            .addToBackStack(null)
            .apply {
                activity.sheetBehavior.state =
                    BottomSheetBehavior.STATE_COLLAPSED
            }
            .commit()
    }
}