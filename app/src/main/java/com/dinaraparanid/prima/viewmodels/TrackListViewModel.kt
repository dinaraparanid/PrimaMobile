package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrackListViewModel : ViewModel() {
    internal val highlightedStartLiveData = MutableLiveData<Boolean>()

    fun load(highlightedStart: Boolean?) {
        highlightedStartLiveData.value = highlightedStart ?: false
    }
}