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
    internal fun getPlaylistSelectorButton() = title !in viewModel.removeSetLiveData.value!!
            && (title in viewModel.addSetLiveData.value!!
            || title in playlistSet)

    /** Adds or removes task to add / remove track */
    @JvmName("onSelectorClicked")
    internal fun onSelectorClicked() = when {
        playlistSelector.isChecked -> viewModel.addSetLiveData.value!!.add(title)

        else -> when (title) {
            in viewModel.addSetLiveData.value!! ->
                viewModel.addSetLiveData.value!!.remove(title)

            else -> viewModel.removeSetLiveData.value!!.add(title)
        }
    }
}