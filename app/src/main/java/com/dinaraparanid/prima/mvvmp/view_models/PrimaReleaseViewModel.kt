package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.PrimaReleasePresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ObservableViewModel] for NewReleaseDialog */

class PrimaReleaseViewModel(presenter: PrimaReleasePresenter) :
    ObservableViewModel<PrimaReleasePresenter>(presenter) {
    // ----------------------------------- Update Button -----------------------------------

    private val _isUpdateButtonPressedState = MutableStateFlow(false)
    val isUpdateButtonPressedState = _isUpdateButtonPressedState.asStateFlow()

    @JvmName("onUpdateButtonPressed")
    fun onUpdateButtonPressed() {
        _isUpdateButtonPressedState.value = true
    }

    fun finishUpdateButtonPressedEvent() {
        _isUpdateButtonPressedState.value = false
    }

    // ----------------------------------- Update Later Button -----------------------------------

    private val _isUpdateLaterButtonPressedState = MutableStateFlow(false)
    val isUpdateLaterButtonPressedState = _isUpdateLaterButtonPressedState.asStateFlow()

    @JvmName("onUpdateLaterButtonPressed")
    fun onUpdateLaterButtonPressed() {
        _isUpdateLaterButtonPressedState.value = true
    }

    fun finishUpdateLaterButtonPressedEvent() {
        _isUpdateLaterButtonPressedState.value = false
    }
}