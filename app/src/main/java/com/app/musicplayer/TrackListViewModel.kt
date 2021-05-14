package com.app.musicplayer

import androidx.lifecycle.ViewModel
import com.app.musicplayer.database.MusicRepository
import java.util.UUID

class TrackListViewModel : ViewModel() {
    private val trackRepository = MusicRepository.getInstance()
    val trackListLiveData = trackRepository.tracks
    val playingTrackId: UUID? = null
}