package com.dinaraparanid.prima.viewmodels.mvvm

import android.widget.CheckBox
import com.dinaraparanid.prima.viewmodels.androidx.PlaylistSelectedViewModel

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.PlaylistSelectFragment]
 */

class PlaylistSelectViewModel(
    private val title: String,
    private val viewModel: PlaylistSelectedViewModel,
    private val playlistSet: Set<String>,
    private val playlistSelector: CheckBox
) : ViewModel() {

    /** Sets playlist's selector button*/
    @JvmName("getPlaylistSelectorButton")
    internal fun getPlaylistSelectorButton() = title !in viewModel.removeSetFlow.value!!
            && (title in viewModel.addSetFlow.value!!
            || title in playlistSet)

    /** Adds or removes task to add / remove track */
    @JvmName("onSelectorClicked")
    internal fun onSelectorClicked() = when {
        playlistSelector.isChecked -> viewModel.addSetFlow.value!!.add(title)

        else -> when (title) {
            in viewModel.addSetFlow.value!! ->
                viewModel.addSetFlow.value!!.remove(title)

            else -> viewModel.removeSetFlow.value!!.add(title)
        }
    }
}