package com.app.musicplayer.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.MusicRepository
import java.util.UUID

class TrackDetailedViewModel : ViewModel() {
    private val musicRepository = MusicRepository.getInstance()
    private val trackIdLiveData = MutableLiveData<UUID>()
    private val isPlayingLiveData = MutableLiveData<Boolean>()

    var trackLiveData = Transformations.switchMap(trackIdLiveData) { trackId ->
        musicRepository.getTrack(trackId)
    }

    fun loadTrack(trackId: UUID) {
        trackIdLiveData.value = trackId
    }

    fun saveTrack(track: Track) = musicRepository.updateTrack(track)
}