package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.RecordParamsPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

/** [ObservableViewModel] for RecordParamsViewModel */

class RecordParamsViewModel : ObservableViewModel<RecordParamsPresenter>() {
    override val presenter by inject<RecordParamsPresenter>()

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