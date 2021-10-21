package com.dinaraparanid.prima.viewmodels.androidx

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist

/** [ViewModel] for [com.dinaraparanid.prima.GuessTheMelodyActivity] */

class GuessTheMelodyActivityViewModel : ViewModel() {
    internal val playlistLiveData = MutableLiveData<AbstractPlaylist>()

    /**
     * Loading params for an activity
     * @param playlist game playlist
     */

    fun load(playlist: AbstractPlaylist?) {
        playlistLiveData.value = playlist ?: DefaultPlaylist()
    }
}