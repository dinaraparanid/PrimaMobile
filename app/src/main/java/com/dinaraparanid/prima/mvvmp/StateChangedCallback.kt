package com.dinaraparanid.prima.mvvmp

import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import kotlinx.coroutines.flow.StateFlow

class StateChangedCallback<H : UIHandler>(
    val uiHandler: H,
    val state: StateFlow<Boolean>,
    val callback: suspend H.() -> Unit
) {
    suspend operator fun invoke() {
        state.collect { isChanged -> if (isChanged) callback(uiHandler) }
    }
}