package com.dinaraparanid.prima.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrackChangeViewModel : ViewModel() {
    internal val albumImagePathLiveData = MutableLiveData<String?>()
    internal val albumImageUriLiveData = MutableLiveData<Uri?>()

    internal fun load(
        albumImagePath: String?,
        albumImageUri: Uri?
    ) {
        albumImagePathLiveData.value = albumImagePath
        albumImageUriLiveData.value = albumImageUri
    }
}