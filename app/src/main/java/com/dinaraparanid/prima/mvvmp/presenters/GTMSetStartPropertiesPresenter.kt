package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for GTMSetStartPropertiesDialogFragment */

class GTMSetStartPropertiesPresenter : GTMSetStartPlaybackPresenter() {
    @get:Bindable
    var gtmTracksAmount = ""
        @JvmName("getGTMTracksAmount") get
        @JvmName("setGTMTracksAmount")
        set(value) {
            field = value
            notifyPropertyChanged(BR.gtmTracksAmount)
        }

    inline val isGTMTracksEnough
        get() = gtmTracksAmount.toIntOrNull()?.let { it > 3 } == true
}