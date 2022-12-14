package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import kotlinx.coroutines.CoroutineScope

/** [InputDialogUIHandler] with no args in [onOkAsync] */

interface NoArgsInputDialogUIHandler : InputDialogUIHandler<NoArgsInputDialogUIHandler.NoArgs> {
    object NoArgs : InputDialogUIHandler.Args

    suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
    )

    override suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: NoArgs?
    ) = onOkAsync(input, dialog)
}