package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for RecordParamsDialog */

class RecordParamsPresenter : BasePresenter() {
    @get:Bindable
    var recordFileName = ""
        @JvmName("getRecordFileName") get
        @JvmName("setRecordFileName")
        set(value) {
            field = value
            notifyPropertyChanged(BR.recordFileName)
        }

    @JvmName("onRecordFileNameInputChanged")
    fun onRecordFileNameInputChanged(txt: String) {
        recordFileName = txt
    }

    @get:Bindable
    var recordSrcSelectedItemPosition = 0
        @JvmName("getRecordSrcSelectedItemPosition") get
        @JvmName("setRecordSrcSelectedItemPosition")
        set(value) {
            field = value
            notifyPropertyChanged(BR.recordSrcSelectedItemPosition)
        }
}