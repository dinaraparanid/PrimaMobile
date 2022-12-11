package com.dinaraparanid.prima.mvvmp.presenters

import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.mvvmp.view.dialogs.TrimmedAudioFileSaveDialog

/** [BasePresenter] for FileSaveDialog */

class TrimmedAudioFileSavePresenter(initialFileName: String) : BasePresenter() {
    @get:Bindable
    var selectedItemPosition = TrimmedAudioFileSaveDialog.FILE_TYPE_RINGTONE
        @JvmName("getSelectedItemPosition") get
        @JvmName("setSelectedItemPosition")
        set(value) {
            field = value
            notifyPropertyChanged(BR.previousSelection)
        }

    @get:Bindable
    var fileName = initialFileName
        @JvmName("getFileName") get
        @JvmName("setFileName")
        set(value) {
            field = value
            notifyPropertyChanged(BR.fileName)
        }
}