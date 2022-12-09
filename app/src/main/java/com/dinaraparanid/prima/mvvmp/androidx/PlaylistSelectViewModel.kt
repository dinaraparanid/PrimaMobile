package com.dinaraparanid.prima.mvvmp.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ViewModel] for
 * [com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment]
 */

class PlaylistSelectViewModel : ViewModel() {
    private val _newSetFlow = MutableStateFlow(hashSetOf<CustomPlaylist.Entity>())

    internal val newSetFlow
        get() = _newSetFlow.asStateFlow()

    /**
     * Loads content for fragment
     * @param newSet List with playlists that will replace current one
     */

    internal fun load(newSet: Array<CustomPlaylist.Entity>) {
        _newSetFlow.value = newSet.toHashSet()
    }
}