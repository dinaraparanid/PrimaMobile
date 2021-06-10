package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrackListViewModel : ViewModel() {
    //internal var playlistLiveData = MutableLiveData<Playlist>()
    // internal var mainLabelOldTextLiveData = MutableLiveData<String>()
    // internal var isMainLiveData = MutableLiveData<Boolean>()
    internal var highlightRowsLiveData = MutableLiveData<MutableList<Int>>()

    fun load(
        // playlist: Playlist?,
        // mainLabelOldText: String?,
        // isMain: Boolean?,
        highlightRows: ArrayList<Int>?
    ) {
        //playlistLiveData.value = playlist ?: Playlist()
        // mainLabelOldTextLiveData.value = mainLabelOldText ?: "Tracks"
        // isMainLiveData.value = isMain ?: true
        highlightRowsLiveData.value = highlightRows ?: mutableListOf()
    }
}