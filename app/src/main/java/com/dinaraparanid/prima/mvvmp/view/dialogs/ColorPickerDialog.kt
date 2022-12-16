package com.dinaraparanid.prima.mvvmp.view.dialogs

import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogColorPickerBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.ColorPickerPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.ColorPickerUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.ColorPickerViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject
import top.defaults.colorpicker.ColorObserver

/** Dialog to choose theme's colors */

class ColorPickerDialog(private val observer: ColorPickerObserver) : ObservableDialogFragment<
        ColorPickerPresenter,
        ColorPickerViewModel,
        ColorPickerUIHandler,
        DialogColorPickerBinding
>() {

    /** Observer that controls color selection */

    abstract class ColorPickerObserver : ColorObserver {
        abstract fun onColorPicked(color: Int)
        override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) = Unit
    }

    override val uiHandler by inject<ColorPickerUIHandler>()
    override val viewModel by viewModel<ColorPickerViewModel>()

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(uiHandler, viewModel.isCancelPressedState) {
                closeDialog(requireDialog())
                viewModel.finishCancel()
            },
            StateChangedCallback(uiHandler, viewModel.isOkPressedState) {
                confirmColorSetup(
                    dialog = requireDialog(),
                    observer = observer,
                    color = viewModel.presenter.colorPickerCurrentColor
                )

                viewModel.finishColorPicking()
            }
        )
    }

    override val dialogBinding
        get() = DataBindingUtil
            .inflate<DialogColorPickerBinding>(
                layoutInflater,
                R.layout.dialog_color_picker,
                null, false
            )
            .apply {
                viewModel = this@ColorPickerDialog.viewModel
                initObservers()
                executePendingBindings()
            }

    private fun DialogColorPickerBinding.initObservers() {
        colorPickerView.run {
            lifecycleOwner = this@ColorPickerDialog.viewLifecycleOwner
            subscribe(observer)
        }

        colorPickerView.subscribe { color, _, _ ->
            viewModel.presenter.colorPickerCurrentColor = color
        }
    }
}