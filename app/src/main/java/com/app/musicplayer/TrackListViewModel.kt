package com.app.musicplayer

import androidx.lifecycle.ViewModel
import com.app.musicplayer.database.MusicRepository

class TrackListViewModel : ViewModel() {
    val trackListLiveData = MusicRepository.getInstance().tracks
}