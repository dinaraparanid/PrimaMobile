package com.dinaraparanid.prima.mvvmp.old_shit

import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.mvvmp.androidx.PlaylistSelectViewModel
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment]
 */

class PlaylistSelectViewModel(
    private val playlist: CustomPlaylist.Entity,
    _viewModel: PlaylistSelectViewModel
) : BasePresenter() {
    @JvmField
    internal val title = playlist.title

    private val _viewModel = WeakReference(_viewModel)
    private inline val viewModel get() = _viewModel.unchecked

    private var _isChecked = isChecked

    /** Sets playlist's selector button*/
    internal inline val isChecked
        @JvmName("isChecked")
        get() = playlist in viewModel.newSetFlow.value

    /** Adds or removes task to add / remove track */
    @JvmName("onPlaylistSelectorClicked")
    internal fun onPlaylistSelectorClicked() {
        _isChecked = !_isChecked

        when {
            _isChecked -> viewModel.newSetFlow.value.add(playlist)
            else -> viewModel.newSetFlow.value.remove(playlist)
        }
    }
}