package com.dinaraparanid.prima.dialogs

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
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ColorPickerBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.CoroutineScope
import top.defaults.colorpicker.ColorObserver
import java.lang.ref.WeakReference
import java.util.Locale

/** Dialog to choose theme's colors */

internal class ColorPickerDialog internal constructor(
    private val activity: WeakReference<MainActivity>,
    private val viewModel: ViewModel
) : AsyncContext {
    private lateinit var popupWindow: PopupWindow
    private val initialColor = Params.instance.primaryColor
    private val onlyUpdateOnTouchEventUp: Boolean = true

    override val coroutineScope: CoroutineScope
        get() = activity.unchecked.lifecycleScope

    /** Shows dialog in a default position */
    internal fun show(observer: ColorPickerObserver) = show(null, observer)

    /**
     * Shows dialog in a parent's position
     * @param parent parent's view on which position dialog will be shown
     */

    @SuppressLint("InflateParams")
    internal fun show(parent: View?, observer: ColorPickerObserver?) {
        val inflater = activity.unchecked
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
                lifecycleOwner = activity.unchecked
            }

            popupWindow = PopupWindow(
                root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setBackgroundDrawable(ColorDrawable(Params.instance.secondaryColor)) }

            cancel.run {
                runOnUIThread {
                    val params = Params.instance
                    typeface = params.getFontFromName(params.font)
                    setTextColor(params.fontColor)
                }

                setOnClickListener { popupWindow.dismiss() }
            }

            ok.run {
                runOnUIThread {
                    val params = Params.instance
                    typeface = params.getFontFromName(params.font)
                    setTextColor(params.fontColor)
                }

                setOnClickListener {
                    runOnUIThread {
                        val params = Params.getInstanceSynchronized()
                        typeface = params.getFontFromName(params.font)
                        popupWindow.dismiss()
                    }

                    observer?.onColorPicked(colorPickerView.color)
                }
            }

            colorHex.text = initialColor.hexTitle()

            colorPickerView.subscribe { color, _, _ ->
                colorIndicator.setBackgroundColor(color)
                colorHex.text = color.hexTitle()
            }
        }

        popupWindow.animationStyle = R.style.TopDefaultsViewColorPickerPopupAnimation
        popupWindow.showAtLocation(parent ?: binding.root, Gravity.CENTER, 0, 0)
    }

    /** Gets hex title from color */
    private fun Int.hexTitle(): String {
        val a = Color.alpha(this)
        val r = Color.red(this)
        val g = Color.green(this)
        val b = Color.blue(this)
        return String.format(Locale.getDefault(), "0x%02X%02X%02X%02X", a, r, g, b)
    }

    /** Observer that controls color selection */
    internal abstract class ColorPickerObserver : ColorObserver {
        abstract fun onColorPicked(color: Int)
        override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) = Unit
    }
}