package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.ColorPickerPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ObservableViewModel] for ColorPickerDialog */

class ColorPickerViewModel(presenter: ColorPickerPresenter) :
    ObservableViewModel<ColorPickerPresenter>(presenter) {
    // --------------------------------- Cancel ---------------------------------

    private val _isCancelPressedState = MutableStateFlow(false)
    val isCancelPressedState = _isCancelPressedState.asStateFlow()

    @JvmName("onCancelPressed")
    fun onCancelPressed() {
        _isCancelPressedState.value = true
    }

    fun finishCancel() {
        _isCancelPressedState.value = false
    }

    // --------------------------------- Ok ---------------------------------

    private val _isOkPressedState = MutableStateFlow(false)
    val isOkPressedState = _isOkPressedState.asStateFlow()

    @JvmName("onOkPressed")
    fun onOkPressed() {
        _isOkPressedState.value = true
    }

    fun finishColorPicking() {
        _isOkPressedState.value = false
    }

    // -----------------------------------------------------------------------------
}