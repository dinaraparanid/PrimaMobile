package com.dinaraparanid.prima.viewmodels.mvvm

import androidx.databinding.BaseObservable
import com.dinaraparanid.prima.utils.Params

/** MVVM View Model ancestor with [Params] */

open class ViewModel : BaseObservable() {
    internal val params = Params.instance
        @JvmName("getParams") get
}