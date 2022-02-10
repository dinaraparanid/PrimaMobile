package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ViewModel] for
 * [com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment]
 */

class PlaylistSelectedViewModel : ViewModel() {
    internal val isAllSelectedFlow = MutableStateFlow(false)
    private val _addSetFlow = MutableStateFlow(mutableSetOf<CustomPlaylist.Entity>())
    private val _removeSetFlow = MutableStateFlow(mutableSetOf<CustomPlaylist.Entity>())

    internal val addSetFlow
        get() = _addSetFlow.asStateFlow()

    internal val removeSetFlow
        get() = _removeSetFlow.asStateFlow()

    /**
     * Loads content for fragment
     * @param selectAll was select all button clicked
     * @param addSet List with playlists to add to
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     * @param removeSet List with playlists to remove from
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     */

    internal fun load(
        selectAll: Boolean?,
        addSet: CustomPlaylist.Entity.EntityList?,
        removeSet: CustomPlaylist.Entity.EntityList?
    ) {
        isAllSelectedFlow.value = selectAll ?: false
        addSet?.let { _addSetFlow.value = it.toMutableSet() }
        removeSet?.let { _removeSetFlow.value = it.toMutableSet() }
    }
}