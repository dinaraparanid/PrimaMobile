package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import kotlinx.coroutines.flow.MutableStateFlow

/** [ViewModel] for TrackListFoundFragment */

class TrackListFoundViewModel : ViewModel() {
    internal val trackListFlow = MutableStateFlow(mutableListOf<GeniusTrack>())

    /**
     * Loads content for fragment
     * @param trackList [Array] with loaded [GeniusTrack]s
     */

    internal fun load(trackList: Array<GeniusTrack>?) {
        trackList?.run { trackListFlow.value = toMutableList() }
    }
}