package com.dinaraparanid.prima.viewmodels.androidx

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack

class TrackChangeViewModel : ViewModel() {
    internal val wasLoadedLiveData = MutableLiveData<Boolean>() // loading for first time
    internal val albumImagePathLiveData = MutableLiveData<String?>()
    internal val albumImageUriLiveData = MutableLiveData<Uri?>()
    internal val trackListLiveData = MutableLiveData<MutableList<GeniusTrack>>()

    internal fun load(
        wasLoaded: Boolean?,
        albumImagePath: String?,
        albumImageUri: Uri?,
        trackList: Array<GeniusTrack>?
    ) {
        wasLoadedLiveData.value = wasLoaded ?: false
        albumImagePathLiveData.value = albumImagePath
        albumImageUriLiveData.value = albumImageUri
        trackListLiveData.value = trackList?.toMutableList() ?: mutableListOf()
    }
}