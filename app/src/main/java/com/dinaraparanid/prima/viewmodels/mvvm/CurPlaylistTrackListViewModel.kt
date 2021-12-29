package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.fragments.playing_panel_fragments.CurPlaylistTrackListFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

class CurPlaylistTrackListViewModel(f: CurPlaylistTrackListFragment) : ViewModel() {
    private val fragment = WeakReference(f)

    /** Shuffles tracks on click */
    @JvmName("onShuffleTracksButtonPressed")
    internal fun onShuffleTracksButtonPressed() =
        fragment.unchecked.onShuffleButtonPressedForPlayingTrackListAsync()
}