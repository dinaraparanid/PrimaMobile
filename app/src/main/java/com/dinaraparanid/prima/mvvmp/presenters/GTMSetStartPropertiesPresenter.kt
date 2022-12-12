package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for GTMSetStartPropertiesDialog */

class GTMSetStartPropertiesPresenter : GTMSetStartPlaybackPresenter() {
    @get:Bindable
    var gtmTracksAmount = ""
        @JvmName("getGTMTracksAmount") get
        @JvmName("setGTMTracksAmount")
        set(value) {
            field = value
            notifyPropertyChanged(BR.gtmTracksAmount)
        }

    @JvmName("onGTMTracksAmountInputChanged")
    fun onGTMTracksAmountInputChanged(txt: String) {
        gtmTracksAmount = txt
    }

    inline val isGTMTracksEnough
        get() = gtmTracksAmount.toIntOrNull()?.let { it > 3 } == true
}