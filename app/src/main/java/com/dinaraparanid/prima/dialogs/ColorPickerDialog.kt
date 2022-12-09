package com.dinaraparanid.prima.dialogs

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ColorPickerBinding
import com.dinaraparanid.prima.fragments.main_menu.settings.SettingsFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.mvvmp.androidx.ColorPickerViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import top.defaults.colorpicker.ColorObserver
import java.lang.ref.WeakReference

/** Dialog to choose theme's colors */

class ColorPickerDialog(fragment: SettingsFragment) : KoinComponent {
    private val fragmentRef by inject<WeakReference<SettingsFragment>> {
        parametersOf(fragment)
    }

    private inline val fragment
        get() = fragmentRef.unchecked

    /** Observer that controls color selection */

    abstract class ColorPickerObserver : ColorObserver {
        abstract fun onColorPicked(color: Int)
        override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) = Unit
    }

    private val viewModel by lazy {
        fragment.getViewModel<ColorPickerViewModel>()
    }

    /** Shows dialog in a default position */
    fun show(observer: ColorPickerObserver): Unit = show(null, observer)

    private fun ColorPickerBinding.initUI(
        observer: ColorPickerObserver,
        window: PopupWindow
    ) {
        colorPickerView.run {
            subscribe(observer)
            lifecycleOwner = fragment
        }

        cancel.setOnClickListener { window.dismiss() }

        ok.setOnClickListener {
            window.dismiss()
            observer.onColorPicked(colorPickerView.color)
        }

        colorPickerView.subscribe { color, _, _ ->
            viewModel.presenter.colorPickerCurrentColor = color
        }

        executePendingBindings()
    }

    private fun getViewBinding(inflater: LayoutInflater) =
        DataBindingUtil
            .inflate<ColorPickerBinding>(inflater, R.layout.color_picker, null, false)
            .apply { viewModel = this@ColorPickerDialog.viewModel }

    private fun ColorPickerWindow(parent: View?, contentView: View) =
        PopupWindow(
            contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setBackgroundDrawable(ColorDrawable(viewModel.secondaryColor))
            animationStyle = R.style.TopDefaultsViewColorPickerPopupAnimation
            showAtLocation(parent ?: contentView, Gravity.CENTER, 0, 0)
        }

    /**
     * Shows dialog in a parent's position
     * @param parent parent's view on which position dialog will be shown
     */

    fun show(parent: View?, observer: ColorPickerObserver) {
        val binding = getViewBinding(
            inflater = fragment
                .requireContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        )

        val window = ColorPickerWindow(parent, contentView = binding.root)
        binding.initUI(observer, window)
        window.showAtLocation(parent ?: binding.root, Gravity.CENTER, 0, 0)
    }
}