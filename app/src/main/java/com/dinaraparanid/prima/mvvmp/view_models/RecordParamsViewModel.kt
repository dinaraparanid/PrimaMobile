package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.RecordParamsPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ObservableViewModel] for RecordParamsViewModel */

class RecordParamsViewModel(presenter: RecordParamsPresenter) :
    ObservableViewModel<RecordParamsPresenter>(presenter) {
    // --------------------------------- SpinnerDropdownIcon ---------------------------------

    private val _isRecordSourceSpinnerDropdownIconPressedState = MutableStateFlow(false)
    val isRecordSourceSpinnerDropdownIconPressedState =
        _isRecordSourceSpinnerDropdownIconPressedState.asStateFlow()

    @JvmName("onRecordSourceSpinnerDropdownIconPressed")
    fun onRecordSourceSpinnerDropdownIconPressed() {
        _isRecordSourceSpinnerDropdownIconPressedState.value = true
    }

    fun finishRecordSourceSetting() {
        _isRecordSourceSpinnerDropdownIconPressedState.value = false
    }

    // --------------------------------- Start Recording Button ---------------------------------

    private val _isStartRecordingButtonPressed = MutableStateFlow(false)
    val isStartRecordingButtonPressed = _isStartRecordingButtonPressed.asStateFlow()

    @JvmName("onStartRecordingButtonPressed")
    fun onStartRecordingButtonPressed() {
        _isStartRecordingButtonPressed.value = true
    }

    fun finishCallingRecorder() {
        _isStartRecordingButtonPressed.value = false
    }

    // ------------------------------------------------------------------------------------------
}