package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.Job

/** Interface for track fragments that can play audio */

internal interface PlayingTrackList<T : AbstractTrack> : AsyncContext {
    suspend fun updateUIForPlayingTrackList(isLocking: Boolean)
    suspend fun loadForPlayingTrackListAsync(): Job
    suspend fun highlightAsync(path: String): Job?

    fun updateUIOnChangeContentForPlayingTrackListAsync(): Job
    fun onShuffleButtonPressedForPlayingTrackListAsync(): Job
}