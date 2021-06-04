package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.database.MusicRepository

class ArtistsViewModel : ViewModel() {
    internal val artistListLiveData: LiveData<List<Artist>> = MusicRepository.getInstance().artists
    internal var mainLabelOldText = MutableLiveData<String>()

    fun load(mainLabelOldText: String?) {
        this.mainLabelOldText.value = mainLabelOldText ?: "Artists"
    }
}