package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment
import com.dinaraparanid.prima.dialogs.NewPlaylistDialog
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/** 
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment]
 */

class PlaylistListViewModel(private val fragment: WeakReference<DefaultPlaylistListFragment>) : ViewModel() {
    /** Shows dialog to add user's playlist */
    @JvmName("onAddPlaylistButtonPressed")
    internal fun onAddPlaylistButtonPressed() = NewPlaylistDialog(fragment.unchecked)
        .show(fragment.unchecked.parentFragmentManager, null)
}