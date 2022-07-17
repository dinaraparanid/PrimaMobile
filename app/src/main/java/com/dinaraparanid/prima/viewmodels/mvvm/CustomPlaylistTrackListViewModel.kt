package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractCustomPlaylistTrackListFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*

/** MVVM View Model for [AbstractCustomPlaylistTrackListFragment] */

class CustomPlaylistTrackListViewModel(
    playlistTitle: String,
    fragment: AbstractCustomPlaylistTrackListFragment,
    private val playlistId: Long,
    private val itemListGetter: suspend () -> List<AbstractTrack>
) : PlaylistTrackListViewModel<FragmentCustomPlaylistTrackListBinding, AbstractCustomPlaylistTrackListFragment>(
    playlistTitle,
    AbstractPlaylist.PlaylistType.CUSTOM.ordinal,
    fragment
), CoroutineScope by MainScope() {

    /** Shows [com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment] */

    @JvmName("onAddTrackButtonClicked")
    internal fun onAddTrackButtonClicked() {
        val tracks = async(Dispatchers.IO) { itemListGetter().toTypedArray() }

        launch(Dispatchers.Main) {
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
                        *tracks.await()
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
}