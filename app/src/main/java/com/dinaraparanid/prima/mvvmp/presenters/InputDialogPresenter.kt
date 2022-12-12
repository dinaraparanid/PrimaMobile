package com.dinaraparanid.prima.mvvmp.presenters

/** [BasePresenter] for InputDialog */

class InputDialogPresenter(
    @JvmField internal val textType: Int,
    @JvmField internal val maxLen: Int = NO_LIMIT_LENGTH
) : BasePresenter() {
    companion object {
        const val NO_LIMIT_LENGTH = -1
    }
}