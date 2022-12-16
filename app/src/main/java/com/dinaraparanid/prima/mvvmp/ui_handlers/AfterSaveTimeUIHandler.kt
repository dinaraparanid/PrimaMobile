package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [InputDialogUIHandler] for AfterSaveTimeDialog */

class AfterSaveTimeUIHandler(private val params: Params) :
    InputDialogUIHandler<AfterSaveTimeUIHandler.AfterSaveTimeUIHandlerArgs> {

    @JvmInline
    value class AfterSaveTimeUIHandlerArgs(val updateAutosaveTimeButtonChannel: Channel<Unit>) :
        InputDialogUIHandler.Args

    override suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: AfterSaveTimeUIHandlerArgs
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

        args.updateAutosaveTimeButtonChannel.send(Unit)
    }
}