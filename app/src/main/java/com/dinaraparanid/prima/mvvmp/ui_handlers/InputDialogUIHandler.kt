package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import kotlinx.coroutines.CoroutineScope

/** [UIHandler] for InputDialogs */

interface InputDialogUIHandler<A : InputDialogUIHandler.Args> : UIHandler {
    /** Arguments besides input in [onOkAsync] */
    interface Args

    suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: A
    )

    suspend fun CoroutineScope.onErrorAsync(input: String) = Unit
}