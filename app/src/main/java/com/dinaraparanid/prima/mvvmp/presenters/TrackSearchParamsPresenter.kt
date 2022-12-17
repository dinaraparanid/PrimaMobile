package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for TrackSearchParamsDialogFragments */

class TrackSearchParamsPresenter(trackTitle: String, artistName: String) : BasePresenter() {
    @get:Bindable
    var searchedTrackTitle = trackTitle
        @JvmName("getSearchedTrackTitle") get
        @JvmName("setSearchedTrackTitle")
        set(value) {
            field = value
            notifyPropertyChanged(BR.searchedTrackTitle)
        }

    @get:Bindable
    var searchedArtistName = artistName
        @JvmName("getSearchedArtistName") get
        @JvmName("setSearchedArtistName")
        set(value) {
            field = value
            notifyPropertyChanged(BR.searchedArtistName)
        }
}