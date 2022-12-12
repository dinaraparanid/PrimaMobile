package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for GTMSetStartPropertiesDialog */

class GTMSetStartPropertiesPresenter : BasePresenter() {
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

    @get:Bindable
    var gtmPlaybackLen = ""
        @JvmName("getGTMPlaybackLen") get
        @JvmName("setGTMPlaybackLen")
        set(value) {
            field = value
            notifyPropertyChanged(BR.gtmPlaybackLen)
        }

    @JvmName("onGTMPlaybackLenInputChanged")
    fun onGTMPlaybackLenInputChanged(txt: String) {
        gtmPlaybackLen = txt
    }

    inline val isGTMPlaybackLenEnough
        get() = gtmPlaybackLen.toByteOrNull()?.let { it > 0 } == true
}