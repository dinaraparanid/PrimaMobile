package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlaylistSelectedViewModel : ViewModel() {
    internal val selectAllLiveData = MutableLiveData<Boolean>()
    internal val addSetLiveData = MutableLiveData<MutableSet<String>>()
    internal val removeSetLiveData = MutableLiveData<MutableSet<String>>()

    fun load(selectAll: Boolean?, addSet: Array<String>?, removeSet: Array<String>?) {
        selectAllLiveData.value = selectAll ?: false
        addSetLiveData.value = addSet?.toMutableSet() ?: mutableSetOf()
        removeSetLiveData.value = removeSet?.toMutableSet() ?: mutableSetOf()
    }
}