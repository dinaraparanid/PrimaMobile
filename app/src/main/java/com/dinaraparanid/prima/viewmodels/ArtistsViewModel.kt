package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.ArtistList

class ArtistsViewModel : ViewModel() {
    internal val artists = MutableLiveData<ArtistList>()
    internal var mainLabelOldText = MutableLiveData<String>()

    fun load(artists: ArtistList?, mainLabelOldText: String?) {
        this.artists.value = artists ?: ArtistList()
        this.mainLabelOldText.value = mainLabelOldText ?: "Artists"
    }
}