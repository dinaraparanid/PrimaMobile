package com.dinaraparanid.prima.mvvmp.view_models

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import org.koin.core.component.KoinComponent

/** [ViewModel] with [BasePresenter] */

abstract class ObservableViewModel<P : BasePresenter> : ViewModel(), KoinComponent {
    abstract val presenter: P

    inline val primaryColor
        @JvmName("getPrimaryColor")
        get() = presenter.primaryColor

    inline val secondaryColor
        @JvmName("getSecondaryColor")
        get() = presenter.secondaryColor

    inline val fontColor
        @JvmName("getFontColor")
        get() = presenter.fontColor

    inline val fontName
        @JvmName("getFontName")
        get() = presenter.fontName

    @RequiresApi(Build.VERSION_CODES.N)
    @JvmName("getBloomOrTransparent")
    fun getBloomOrTransparent(color: Int) = presenter.getBloomOrTransparent(color)

    @JvmName("getFont")
    fun getFont() = presenter.getFont()
}