package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for GTMSetPropertiesDialogFragments */

open class GTMSetStartPlaybackPresenter : BasePresenter() {
    @get:Bindable
    var gtmPlaybackLen = ""
        @JvmName("getGTMPlaybackLen") get
        @JvmName("setGTMPlaybackLen")
        set(value) {
            field = value
            notifyPropertyChanged(BR.gtmPlaybackLen)
        }

    inline val isGTMPlaybackLenEnough
        get() = gtmPlaybackLen.toByteOrNull()?.let { it > 0 } == true
}