package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.MusicRepository

class TrackListViewModel : ViewModel() {
    val trackListLiveData: LiveData<List<Track>> = MusicRepository.getInstance().tracks
}