package com.dinaraparanid.prima.mvvmp.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.entities.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ViewModel] for
 * [com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment]
 */

class TrackSelectViewModel : ViewModel() {
    private val _newSetFlow = MutableStateFlow(hashSetOf<Track>())

    internal val newSetFlow
        get() = _newSetFlow.asStateFlow()

    /**
     * Loads content for fragment
     * @param newSet [Array] of current tracks that are in the new set
     */

    internal fun load(newSet: Array<Track>) {
        _newSetFlow.value = newSet.toHashSet()
    }
}