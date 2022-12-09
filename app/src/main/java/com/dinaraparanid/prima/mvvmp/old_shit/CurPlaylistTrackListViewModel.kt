package com.dinaraparanid.prima.mvvmp.old_shit

import com.dinaraparanid.prima.fragments.playing_panel_fragments.CurPlaylistTrackListFragment
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

class CurPlaylistTrackListViewModel(f: CurPlaylistTrackListFragment) : BasePresenter() {
    private val fragment = WeakReference(f)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() =
        fragment.unchecked.onShuffleButtonPressedForPlayingTrackListAsync()
}