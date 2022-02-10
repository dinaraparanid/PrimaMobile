package com.dinaraparanid.prima.viewmodels.mvvm

import android.widget.CheckBox
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.viewmodels.androidx.PlaylistSelectedViewModel
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment]
 */

class PlaylistSelectViewModel(
    private val playlist: CustomPlaylist.Entity,
    _viewModel: PlaylistSelectedViewModel,
    private val playlistSet: Set<CustomPlaylist.Entity>,
    _playlistSelector: CheckBox
) : ViewModel() {
    @JvmField
    internal val title = playlist.title

    private val _viewModel = WeakReference(_viewModel)
    private inline val viewModel get() = _viewModel.unchecked

    private val _playlistSelector = WeakReference(_playlistSelector)
    private inline val playlistSelector get() = _playlistSelector.unchecked

    /** Sets playlist's selector button*/
    @JvmName("getPlaylistSelectorButton")
    internal fun getPlaylistSelectorButton() = playlist !in viewModel.removeSetFlow.value!!
            && (playlist in viewModel.addSetFlow.value!!
            || playlist in playlistSet)

    /** Adds or removes task to add / remove track */
    @JvmName("onSelectorClicked")
    internal fun onSelectorClicked() = when {
        playlistSelector.isChecked -> viewModel.addSetFlow.value!!.add(playlist)

        else -> when (playlist) {
            in viewModel.addSetFlow.value!! ->
                viewModel.addSetFlow.value!!.remove(playlist)

            else -> viewModel.removeSetFlow.value!!.add(playlist)
        }
    }
}