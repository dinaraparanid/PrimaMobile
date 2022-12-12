package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for GTMSetPropertiesDialogs */

open class GTMSetStartPlaybackPresenter : BasePresenter() {
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