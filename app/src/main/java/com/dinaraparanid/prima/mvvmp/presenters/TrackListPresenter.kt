package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for TrackListFragments */

class TrackListPresenter(initialNumberOfTracks: Int, recyclerViewBottomMargin: Int) :
    MainActivityListPresenter(recyclerViewBottomMargin) {
    @get:Bindable
    var numberOfTracks = initialNumberOfTracks
        @JvmName("getNumberOfTracks") get
        @JvmName("setNumberOfTracks")
        set(value) {
            field = value
            notifyPropertyChanged(BR.numberOfTracks)
        }
}