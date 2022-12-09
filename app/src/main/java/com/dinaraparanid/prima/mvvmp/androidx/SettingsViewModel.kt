package com.dinaraparanid.prima.mvvmp.androidx

import com.dinaraparanid.prima.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dinaraparanid.prima.mvvmp.old_shit.SettingsObserver

class SettingsViewModel(settingsObserver: SettingsObserver) :
    ObservableViewModel<SettingsObserver>(settingsObserver) {

    // ------------------------------ Change Language ------------------------------

    private val _isLangChangeInvokedState = MutableStateFlow(false)
    val isLangChangeInvokedState = _isLangChangeInvokedState.asStateFlow()

    /** Changes language and restarts [MainActivity] */
    @JvmName("onLanguageButtonPressed")
    fun onLanguageButtonPressed() {
        _isLangChangeInvokedState.value = true
    }

    fun finishLangChange() {
        _isLangChangeInvokedState.value = false
    }

    // ------------------------------ Change Font ------------------------------

    private val _isFontChangeInvokedState = MutableStateFlow(false)
    val isFontChangeInvokedState = _isFontChangeInvokedState.asStateFlow()

    @JvmName("onFontButtonPressed")
    fun onFontButtonPressed() {
        _isFontChangeInvokedState.value = true
    }

    fun finishFontChange() {
        _isFontChangeInvokedState.value = false
    }

    // ------------------------------ Change Text Color ------------------------------

    private val _isTextColorChangeInvokedState = MutableStateFlow(false)
    val isTextColorChangeInvokedState = _isTextColorChangeInvokedState.asStateFlow()

    @JvmName("onTextColorButtonPressed")
    fun onTextColorButtonPressed() {
        _isFontChangeInvokedState.value = true
    }

    fun finishTextColorChange() {
        _isTextColorChangeInvokedState.value = false
    }

    // ------------------------------ Change Theme Color ------------------------------

    private val _isThemeColorChangeInvokedState = MutableStateFlow(false)
    val isThemeColorChangeInvokedState = _isThemeColorChangeInvokedState.asStateFlow()

    @JvmName("onThemeButtonPressed")
    fun onThemeButtonPressed() {
        _isThemeColorChangeInvokedState.value = true
    }

    fun finishThemeColorChange() {
        _isThemeColorChangeInvokedState.value = false
    }

    // ------------------------------ Hide / Show Cover ------------------------------

    private val _isCoverShownChangeInvokedState = MutableStateFlow(false)
    val isCoverShownChangeInvokedState = _isCoverShownChangeInvokedState.asStateFlow()

    @JvmName("onHideCoverButtonClicked")
    fun onHideCoverButtonClicked() {
        _isCoverShownChangeInvokedState.value = true
    }

    fun finishCoverShownChange() {
        _isCoverShownChangeInvokedState.value = false
    }

    // ------------------------------ Display Cover ------------------------------

    private val _areCoversDisplayedInvokedState = MutableStateFlow(false)
    val areCoversDisplayedInvokedState = _areCoversDisplayedInvokedState.asStateFlow()

    @JvmName("onDisplayCoversButtonClicked")
    fun onDisplayCoversButtonClicked() {
        _areCoversDisplayedInvokedState.value = true
    }

    fun finishDisplayCoversChange() {
        _areCoversDisplayedInvokedState.value = false
    }

    // ------------------------------ Rotate Cover ------------------------------

    private val _isCoverRotateChangeInvokedState = MutableStateFlow(false)
    val isCoverRotateChangeInvokedState = _isCoverRotateChangeInvokedState.asStateFlow()

    @JvmName("onCoverRotateButtonClicked")
    fun onCoverRotateButtonClicked() {
        _isCoverRotateChangeInvokedState.value = true
    }

    fun finishCoverRotateChange() {
        _isCoverRotateChangeInvokedState.value = false
    }

    // ------------------------------ Cover Rounding ------------------------------

    private val _areCoversRoundedChangeInvokedState = MutableStateFlow(false)
    val areCoversRoundedChangeInvokedState = _areCoversRoundedChangeInvokedState.asStateFlow()

    @JvmName("onPlaylistImageCirclingButtonClicked")
    fun onPlaylistImageCirclingButtonClicked() {
        _areCoversRoundedChangeInvokedState.value = true
    }

    fun finishCoverRoundedChange() {
        _areCoversRoundedChangeInvokedState.value = false
    }
}