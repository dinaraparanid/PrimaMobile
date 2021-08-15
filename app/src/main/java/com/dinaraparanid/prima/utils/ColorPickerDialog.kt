package com.dinaraparanid.prima.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ColorPickerBinding
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import top.defaults.colorpicker.ColorObserver
import java.util.*

internal class ColorPickerDialog internal constructor(
    private val context: Context,
    private val viewModel: ViewModel
) {
    private lateinit var popupWindow: PopupWindow
    private val initialColor = Params.instance.primaryColor
    private val onlyUpdateOnTouchEventUp: Boolean = true

    internal fun show(observer: ColorPickerObserver) = show(null, observer)

    @SuppressLint("InflateParams")
    internal fun show(parent: View?, observer: ColorPickerObserver?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val binding = DataBindingUtil.inflate<ColorPickerBinding>(
            inflater,
            R.layout.color_picker,
            null,
            false
        ).apply {
            viewModel = this@ColorPickerDialog.viewModel

            colorPickerView.run {
                setInitialColor(initialColor)
                setEnabledBrightness(true)
                setEnabledAlpha(true)
                setOnlyUpdateOnTouchEventUp(onlyUpdateOnTouchEventUp)
                subscribe(observer)
            }

            popupWindow = PopupWindow(
                root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setBackgroundDrawable(ColorDrawable(Params.instance.secondaryColor))
                isOutsideTouchable = true
            }

            cancel.run {
                typeface = Params.instance.getFontFromName(Params.instance.font)
                setTextColor(Params.instance.fontColor)
                setOnClickListener { popupWindow.dismiss() }
            }

            ok.run {
                typeface = Params.instance.getFontFromName(Params.instance.font)
                setTextColor(Params.instance.fontColor)
                setOnClickListener {
                    typeface = Params.instance.getFontFromName(Params.instance.font)
                    popupWindow.dismiss()
                    observer?.onColorPicked(colorPickerView.color)
                }
            }

            colorHex.text = colorHex(initialColor)

            colorPickerView.subscribe { color, _, _ ->
                colorIndicator.setBackgroundColor(color)
                colorHex.text = colorHex(color)
            }
        }

        popupWindow.animationStyle = R.style.TopDefaultsViewColorPickerPopupAnimation
        popupWindow.showAtLocation(parent ?: binding.root, Gravity.CENTER, 0, 0)
    }

    private fun colorHex(color: Int): String {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return String.format(Locale.getDefault(), "0x%02X%02X%02X%02X", a, r, g, b)
    }

    internal abstract class ColorPickerObserver : ColorObserver {
        abstract fun onColorPicked(color: Int)
        override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {}
    }
}