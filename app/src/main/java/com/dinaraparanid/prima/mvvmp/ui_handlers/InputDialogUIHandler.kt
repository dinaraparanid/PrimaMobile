package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface

/** [UIHandler] for InputDialogs */

interface InputDialogUIHandler<A : InputDialogUIHandler.Args> : UIHandler {
    /** Arguments besides input in [onOkAsync] */
    interface Args

    suspend fun A.onOkAsync(
        input: String,
        dialog: DialogInterface
    )

    suspend fun A.onErrorAsync(input: String) = Unit
}