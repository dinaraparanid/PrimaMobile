package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.database.MusicRepository

class TrackListViewModel : ViewModel() {
    val trackListLiveData = MusicRepository.getInstance().tracks
}