package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrackListViewModel : ViewModel() {
    internal val highlightRowsLiveData = MutableLiveData<MutableList<String>>()
    internal val highlightedStartLiveData = MutableLiveData<Boolean>()

    fun load(highlightRows: ArrayList<String>?, highlightedStart: Boolean?) {
        highlightRowsLiveData.value = highlightRows ?: mutableListOf()
        highlightedStartLiveData.value = highlightedStart ?: false
    }
}