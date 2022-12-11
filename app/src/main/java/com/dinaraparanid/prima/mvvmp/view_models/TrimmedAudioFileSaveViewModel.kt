package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.TrimmedAudioFileSavePresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** [ObservableViewModel] for TrimmedAudioFileSaveDialog */

class TrimmedAudioFileSaveViewModel(initialFileName: String) :
    ObservableViewModel<TrimmedAudioFileSavePresenter>() {
    override val presenter by inject<TrimmedAudioFileSavePresenter> {
        parametersOf(initialFileName)
    }

    // --------------------------------- Save File Button ---------------------------------

    private val _isSaveFileButtonPressedState = MutableStateFlow(false)
    val isSaveFileButtonPressedState = _isSaveFileButtonPressedState.asStateFlow()

    @JvmName("onSaveFileButtonPressed")
    fun onSaveFileButtonPressed() {
        _isSaveFileButtonPressedState.value = true
    }

    fun finishSavingFile() {
        _isSaveFileButtonPressedState.value = false
    }

    // --------------------------------- Cancel Saving Button ---------------------------------

    private val _isCancelSavingButtonPressedState = MutableStateFlow(false)
    val isCancelSavingButtonPressedState = _isSaveFileButtonPressedState.asStateFlow()

    @JvmName("onCancelSavingButtonPressed")
    fun onCancelSavingButtonPressed() {
        _isCancelSavingButtonPressedState.value = true
    }

    fun finishCancelSaving() {
        _isCancelSavingButtonPressedState.value = false
    }
}