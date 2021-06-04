package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.database.MusicRepository

class TrackListViewModel : ViewModel() {
    internal val trackListLiveData: LiveData<List<Track>> = MusicRepository.getInstance().tracks
    internal var playlist = MutableLiveData<Option<Playlist>>()
    internal var mainLabelOldText = MutableLiveData<String>()

    fun load(playlist: Playlist?, mainLabelOldText: String?) {
        this.playlist.value = playlist?.let { Some(it) } ?: None
        this.mainLabelOldText.value = mainLabelOldText ?: "Tracks"
    }
}