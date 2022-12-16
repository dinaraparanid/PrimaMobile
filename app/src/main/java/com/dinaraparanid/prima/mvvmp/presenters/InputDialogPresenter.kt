package com.dinaraparanid.prima.mvvmp.presenters

/** [BasePresenter] for InputDialog */

class InputDialogPresenter(
    @JvmField val textType: Int,
    @JvmField val maxLen: Int = NO_LIMIT_LENGTH
) : BasePresenter() {
    companion object {
        const val NO_LIMIT_LENGTH = -1
    }
}