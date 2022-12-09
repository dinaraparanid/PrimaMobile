package com.dinaraparanid.prima.mvvmp.view_models

import com.dinaraparanid.prima.mvvmp.presenters.InputDialogPresenter
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class InputDialogViewModel(
    @JvmField internal val textType: Int,
    @JvmField internal val maxLen: Int = InputDialogPresenter.NO_LIMIT_LENGTH
) : ObservableViewModel<InputDialogPresenter>() {
    override val presenter by inject<InputDialogPresenter> {
        parametersOf(textType, maxLen)
    }
}