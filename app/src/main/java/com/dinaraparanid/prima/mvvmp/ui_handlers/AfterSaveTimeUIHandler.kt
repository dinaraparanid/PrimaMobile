package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [InputDialogUIHandler] for AfterSaveTimeDialog */

class AfterSaveTimeUIHandler(private val params: Params) :
    InputDialogUIHandler<AfterSaveTimeUIHandler.Args> {
    @JvmInline
    value class Args(val updateAutosaveTimeButtonChannel: Channel<Unit>) : InputDialogUIHandler.Args

    override suspend fun Args.onOkAsync(
        input: String,
        dialog: DialogInterface,
    ) = coroutineScope {
        input
            .toInt()
            .takeIf { it >= 5 }
            ?.let {
                params.autoSaveTime.set(it)

                launch(Dispatchers.IO) {
                    StorageUtil
                        .getInstanceAsyncSynchronized()
                        .storeAutoSaveTime(it)
                }
            }
            ?: throw Exception()

        updateAutosaveTimeButtonChannel.send(Unit)
    }
}