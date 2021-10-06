package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack

/**
 * [ViewModel] for [com.dinaraparanid.prima.fragments.TrackSelectLyricsFragment]
 */

class TrackSelectLyricsViewModel : ViewModel() {
    internal val trackListLiveData = MutableLiveData<MutableList<GeniusTrack>>()
    
    fun load(trackList: Array<GeniusTrack>?) {
        trackListLiveData.value = trackList?.toMutableList() ?: mutableListOf()
    }
}