package com.dinaraparanid.prima.mvvmp.view.fragments

import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import kotlinx.coroutines.Job

/** Interface for track fragments that can play audio */

@Deprecated("Shit")
interface PlayingTrackList<T : Track> : AsyncContext {
    @Deprecated("Shit")
    suspend fun updateUIForPlayingTrackList(isLocking: Boolean)

    @Deprecated("Shit")
    suspend fun loadForPlayingTrackListAsync(): Job

    @Deprecated("Shit")
    suspend fun highlightAsync(path: String): Job?

    @Deprecated("Shit")
    fun updateUIOnChangeContentForPlayingTrackListAsync(): Job

    @Deprecated("Shit")
    fun onShuffleButtonPressedForPlayingTrackListAsync(): Job
}