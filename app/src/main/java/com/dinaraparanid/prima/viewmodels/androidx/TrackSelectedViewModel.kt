package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.AbstractTrack

/**
 * [ViewModel] for [com.dinaraparanid.prima.fragments.TrackSelectFragment]
 */

class TrackSelectedViewModel : ViewModel() {
    internal val selectAllLiveData = MutableLiveData<Boolean>()
    internal val addSetLiveData = MutableLiveData<MutableSet<AbstractTrack>>()
    internal val removeSetLiveData = MutableLiveData<MutableSet<AbstractTrack>>()

    /**
     * Loads content for fragment
     * @param selectAll was select all button clicked
     * @param addSet [Array] with tracks to add to
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     * @param removeSet [Array] with tracks to remove from
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     */

    fun load(selectAll: Boolean?, addSet: Array<AbstractTrack>?, removeSet: Array<AbstractTrack>?) {
        selectAllLiveData.value = selectAll ?: false
        addSetLiveData.value = addSet?.toMutableSet() ?: mutableSetOf()
        removeSetLiveData.value = removeSet?.toMutableSet() ?: mutableSetOf()
    }
}