package com.dinaraparanid.prima.mvvmp.presenters

import android.graphics.Color
import androidx.databinding.Bindable
import com.dinaraparanid.prima.BR
import java.util.Locale

/** [BasePresenter] for ColorPickerDialog */

class ColorPickerPresenter : BasePresenter() {
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