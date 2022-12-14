package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** [InputDialogUIHandler] for AfterSaveTimeDialog */

class AfterSaveTimeUIHandler :
    InputDialogUIHandler<AfterSaveTimeUIHandler.AfterSaveTimeUIHandlerArgs>, KoinComponent {

    @JvmInline
    value class AfterSaveTimeUIHandlerArgs(val updateAutosaveTimeButtonChannel: Channel<Unit>) :
        InputDialogUIHandler.Args

    private val params by inject<Params>()
    private val storageUtil by inject<StorageUtil>()

    override suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: AfterSaveTimeUIHandlerArgs
    ) {
        input
            .toInt()
            .takeIf { it >= 5 }
            ?.let {
                params.autoSaveTime.set(it)
                storageUtil.storeAutoSaveTime(it)
            }
            ?: throw Exception()

        args.updateAutosaveTimeButtonChannel.send(Unit)
    }
}