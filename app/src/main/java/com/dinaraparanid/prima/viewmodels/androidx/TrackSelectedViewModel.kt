package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [ViewModel] for [com.dinaraparanid.prima.fragments.TrackSelectFragment]
 */

class TrackSelectedViewModel : ViewModel() {
    internal val selectAllFlow = MutableStateFlow(false)
    internal val addSetFlow = MutableStateFlow(mutableSetOf<AbstractTrack>())
    internal val removeSetFlow = MutableStateFlow(mutableSetOf<AbstractTrack>())

    /**
     * Loads content for fragment
     * @param selectAll was select all button clicked
     * @param addSet [Array] with tracks to add to
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     * @param removeSet [Array] with tracks to remove from
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     */

    fun load(selectAll: Boolean?, addSet: Array<AbstractTrack>?, removeSet: Array<AbstractTrack>?) {
        selectAllFlow.value = selectAll ?: false
        addSetFlow.value = addSet?.toMutableSet() ?: mutableSetOf()
        removeSetFlow.value = removeSet?.toMutableSet() ?: mutableSetOf()
    }
}