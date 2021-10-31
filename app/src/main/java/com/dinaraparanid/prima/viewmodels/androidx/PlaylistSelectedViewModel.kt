package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ViewModel] for [com.dinaraparanid.prima.fragments.PlaylistSelectFragment]
 */

class PlaylistSelectedViewModel : ViewModel() {
    internal val isAllSelectedFlow = MutableStateFlow(false)
    private val _addSetFlow = MutableStateFlow(mutableSetOf<String>())
    private val _removeSetFlow = MutableStateFlow(mutableSetOf<String>())

    internal val addSetFlow
        get() = _addSetFlow.asStateFlow()

    internal val removeSetFlow
        get() = _removeSetFlow.asStateFlow()

    /**
     * Loads content for fragment
     * @param selectAll was select all button clicked
     * @param addSet [Array] with playlists' titles to add to
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     * @param removeSet [Array] with playlists' titles to remove from
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     */

    fun load(selectAll: Boolean?, addSet: Array<String>?, removeSet: Array<String>?) {
        isAllSelectedFlow.value = selectAll ?: false
        addSet?.let { _addSetFlow.value = it.toMutableSet() }
        removeSet?.let { _removeSetFlow.value = it.toMutableSet() }
    }
}