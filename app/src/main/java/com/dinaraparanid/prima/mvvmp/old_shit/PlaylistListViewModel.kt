package com.dinaraparanid.prima.mvvmp.old_shit

import com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment
import com.dinaraparanid.prima.dialogs.NewPlaylistDialog
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/** 
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment]
 */

class PlaylistListViewModel(private val fragment: WeakReference<DefaultPlaylistListFragment>) : BasePresenter() {
    /** Shows dialog to add user's playlist */
    @JvmName("onAddPlaylistButtonPressed")
    internal fun onAddPlaylistButtonPressed() = NewPlaylistDialog(fragment.unchecked)
        .show(fragment.unchecked.parentFragmentManager, null)
}