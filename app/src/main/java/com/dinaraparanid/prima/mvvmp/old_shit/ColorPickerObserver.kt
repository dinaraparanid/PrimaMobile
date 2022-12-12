package com.dinaraparanid.prima.mvvmp.old_shit

import android.graphics.Color
import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import java.util.Locale

/** View observer that handles changes of color */

class ColorPickerObserver() : BasePresenter() {
    private companion object {
        /** Gets hex title from color */
        private inline val Int.hexTitle: String
            get() {
                val a = Color.alpha(this)
                val r = Color.red(this)
                val g = Color.green(this)
                val b = Color.blue(this)
                return String.format(Locale.getDefault(), "0x%02X%02X%02X%02X", a, r, g, b)
            }
    }

    @get:Bindable
    var colorPickerCurrentColor = primaryColor
        @JvmName("getColorPickerCurrentColor") get
        @JvmName("setColorPickerCurrentColor")
        set(value) {
            field = value
            notifyPropertyChanged(BR.colorPickerCurrentColor)
        }

    val colorText get() = colorPickerCurrentColor.hexTitle
}