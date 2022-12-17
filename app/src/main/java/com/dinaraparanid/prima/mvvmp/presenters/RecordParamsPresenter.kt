package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR

/** [BasePresenter] for RecordParamsDialogFragment */

class RecordParamsPresenter : BasePresenter() {
    @get:Bindable
    var recordFileName = ""
        @JvmName("getRecordFileName") get
        @JvmName("setRecordFileName")
        set(value) {
            field = value
            notifyPropertyChanged(BR.recordFileName)
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