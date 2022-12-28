package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/**
 * [BasePresenter] that handles margin of recycler view.
 * When playing bar is not shown (means that user have never
 * listened to any track in app), there will be a default margin
 */

open class MainActivityListPresenter(recyclerViewBottomMargin: Int) : BasePresenter() {
    @get:Bindable
    var recyclerViewBottomMargin = recyclerViewBottomMargin
        @JvmName("getRecyclerViewBottomMargin") get
        @JvmName("setRecyclerViewBottomMargin")
        set(value) {
            field = value
            notifyPropertyChanged(BR.recyclerViewBottomMargin)
        }

}