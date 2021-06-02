package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.MusicRepository
import java.util.UUID

class MainActivityViewModel : ViewModel() {
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