package com.dinaraparanid.prima.mvvmp.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ViewModel] for [com.dinaraparanid.prima.GuessTheMelodyActivity] */

class GuessTheMelodyActivityViewModel : ViewModel() {
    private val _playlistFlow = MutableStateFlow<AbstractPlaylist>(DefaultPlaylist())
    private val _maxPlaybackLengthFlow = MutableStateFlow<Byte>(0)

    internal val playlistFlow
        get() = _playlistFlow.asStateFlow()

    internal val maxPlaybackLengthFlow
        get() = _maxPlaybackLengthFlow.asStateFlow()

    /**
     * Loading params for an activity
     * @param tracks game playlist
     * @param maxPlaybackLength maximum playback length
     */

    internal fun load(tracks: Array<AbstractTrack>, maxPlaybackLength: Byte) {
        _playlistFlow.value.run {
            clear()
            addAll(tracks)
        }
        _maxPlaybackLengthFlow.value = maxPlaybackLength
    }
}