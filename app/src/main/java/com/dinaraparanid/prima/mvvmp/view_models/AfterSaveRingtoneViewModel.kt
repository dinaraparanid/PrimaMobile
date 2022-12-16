package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ObservableViewModel] for AfterSaveRingtoneDialog */

class AfterSaveRingtoneViewModel(presenter: BasePresenter) :
    ObservableViewModel<BasePresenter>(presenter) {
    // --------------------------------- Default Button ---------------------------------

    private val _isMakeDefaultButtonPressedState = MutableStateFlow(false)
    val isMakeDefaultButtonPressedState = _isMakeDefaultButtonPressedState.asStateFlow()

    @JvmName("onMakeDefaultButtonPressed")
    fun onMakeDefaultButtonPressed() {
        _isMakeDefaultButtonPressedState.value = true
    }

    fun finishSettingDefaultRingtone() {
        _isMakeDefaultButtonPressedState.value = false
    }

    // --------------------------------- Choose Button ---------------------------------

    private val _isChooseContactButtonPressedState = MutableStateFlow(false)
    val isChooseContactButtonPressedState = _isChooseContactButtonPressedState.asStateFlow()

    @JvmName("onChooseContactButtonPressed")
    fun onChooseContactButtonPressed() {
        _isChooseContactButtonPressedState.value = true
    }

    fun finishSettingContactRingtone() {
        _isChooseContactButtonPressedState.value = false
    }

    // --------------------------------- Do Nothing Button ---------------------------------

    private val _isDoNothingButtonPressedState = MutableStateFlow(false)
    val isDoNothingButtonPressedState = _isChooseContactButtonPressedState.asStateFlow()

    @JvmName("onDoNothingButtonPressed")
    fun onDoNothingButtonPressed() {
        _isDoNothingButtonPressedState.value = true
    }

    fun finishIgnoreResultingRingtone() {
        _isDoNothingButtonPressedState.value = false
    }

    // -------------------------------------------------------------------------------------
}