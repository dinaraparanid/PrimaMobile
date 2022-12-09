package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import kotlinx.coroutines.CoroutineScope

/** [UIHandler] for InputDialogs */

interface InputDialogUIHandler : UIHandler {
    suspend fun CoroutineScope.onOkAsync(input: String, dialog: DialogInterface)
    suspend fun CoroutineScope.onErrorAsync(input: String) = Unit
}