package com.dinaraparanid.prima.utils.polymorphism

import com.dinaraparanid.prima.entities.Track
import kotlinx.coroutines.Job

/** Interface for track fragments that can play audio */

internal interface PlayingTrackList<T : Track> : AsyncContext {
    suspend fun updateUIForPlayingTrackList(isLocking: Boolean)
    suspend fun loadForPlayingTrackListAsync(): Job
    suspend fun highlightAsync(path: String): Job?

    fun updateUIOnChangeContentForPlayingTrackListAsync(): Job
    fun onShuffleButtonPressedForPlayingTrackListAsync(): Job
}