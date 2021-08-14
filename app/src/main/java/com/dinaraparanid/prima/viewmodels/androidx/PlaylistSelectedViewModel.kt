package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * [ViewModel] for [com.dinaraparanid.prima.fragments.PlaylistSelectFragment]
 */

class PlaylistSelectedViewModel : ViewModel() {
    internal val selectAllLiveData = MutableLiveData<Boolean>()
    internal val addSetLiveData = MutableLiveData<MutableSet<String>>()
    internal val removeSetLiveData = MutableLiveData<MutableSet<String>>()

    /**
     * Loads content for fragment
     * @param selectAll was select all button clicked
     * @param addSet [Array] with playlists' titles to add to
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     * @param removeSet [Array] with playlists' titles to remove from
     * [com.dinaraparanid.prima.databases.databases.CustomPlaylistsDatabase]
     */

    fun load(selectAll: Boolean?, addSet: Array<String>?, removeSet: Array<String>?) {
        selectAllLiveData.value = selectAll ?: false
        addSetLiveData.value = addSet?.toMutableSet() ?: mutableSetOf()
        removeSetLiveData.value = removeSet?.toMutableSet() ?: mutableSetOf()
    }
}