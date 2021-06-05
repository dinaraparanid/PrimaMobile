package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.Playlist

class TrackListViewModel : ViewModel() {
    internal var playlist = MutableLiveData<Playlist>()
    internal var mainLabelOldText = MutableLiveData<String>()
    internal var isMain = MutableLiveData<Boolean>()

    fun load(playlist: Playlist?, mainLabelOldText: String?, isMain: Boolean?) {
        this.playlist.value = playlist ?: Playlist()
        this.mainLabelOldText.value = mainLabelOldText ?: "Tracks"
        this.isMain.value = isMain ?: true
    }
}