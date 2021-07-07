package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.Track

class TrackSelectedViewModel : ViewModel() {
    internal val selectAllLiveData = MutableLiveData<Boolean>()
    internal val addSetLiveData = MutableLiveData<MutableSet<Track>>()
    internal val removeSetLiveData = MutableLiveData<MutableSet<Track>>()

    fun load(selectAll: Boolean?, addSet: Array<Track>?, removeSet: Array<Track>?) {
        selectAllLiveData.value = selectAll ?: false
        addSetLiveData.value = addSet?.toMutableSet() ?: mutableSetOf()
        removeSetLiveData.value = removeSet?.toMutableSet() ?: mutableSetOf()
    }
}