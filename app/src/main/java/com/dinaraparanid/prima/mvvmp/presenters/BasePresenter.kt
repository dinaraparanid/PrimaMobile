package com.dinaraparanid.prima.mvvmp.presenters

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.databinding.BaseObservable
import com.dinaraparanid.prima.utils.Params
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** View presenter that handles UI changes via data binding */

open class BasePresenter : BaseObservable(), KoinComponent {
    protected val params by inject<Params>()

    val primaryColor
        @JvmName("getPrimaryColor")
        get() = params.primaryColor

    val secondaryColor
        @JvmName("getSecondaryColor")
        get() = params.secondaryColor

    val fontColor
        @JvmName("getFontColor")
        get() = params.fontColor

    val fontName
        @JvmName("getFontName")
        get() = params.font

    @RequiresApi(Build.VERSION_CODES.N)
    @JvmName("getBloomOrTransparent")
    fun getBloomOrTransparent(color: Int) = params.getBloomOrTransparent(color)

    @JvmName("getFont")
    fun getFont() = params.getFontFromName(fontName)
}