package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogColorPickerBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.ColorPickerPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.ColorPickerUIHandler
import com.dinaraparanid.prima.mvvmp.view.ObservableView
import com.dinaraparanid.prima.mvvmp.view.handleUIStatesChanges
import com.dinaraparanid.prima.mvvmp.view_models.ColorPickerViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.inject
import top.defaults.colorpicker.ColorObserver

/** Dialog to choose theme's colors */

class ColorPickerDialog(fragment: Fragment, private val observer: ColorPickerObserver) :
    DialogFragment(),
    ObservableView<ColorPickerPresenter, ColorPickerViewModel, ColorPickerUIHandler, DialogColorPickerBinding> {

    /** Observer that controls color selection */

    abstract class ColorPickerObserver : ColorObserver {
        abstract fun onColorPicked(color: Int)
        override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) = Unit
    }

    override lateinit var binding: DialogColorPickerBinding

    override val uiHandler by inject<ColorPickerUIHandler>()

    override val viewModel by lazy {
        fragment.getViewModel<ColorPickerViewModel>()
    }

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(uiHandler, viewModel.isCancelPressedState) {
                closeDialog(requireDialog())
            },
            StateChangedCallback(uiHandler, viewModel.isOkPressedState) {
                confirmColorSetup(
                    dialog = requireDialog(),
                    observer = observer,
                    color = viewModel.presenter.colorPickerCurrentColor
                )
            }
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = dialogColorPickerBinding
        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            handleUIStatesChanges(viewLifecycleOwner)
        }
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

    private inline val dialogColorPickerBinding
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
}