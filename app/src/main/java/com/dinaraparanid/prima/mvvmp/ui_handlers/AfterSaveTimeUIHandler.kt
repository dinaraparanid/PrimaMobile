package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** [InputDialogUIHandler] for AfterSaveTimeDialog */

class AfterSaveTimeUIHandler(private val updateAutosaveTimeButtonChannel: Channel<Unit>) :
    InputDialogUIHandler, KoinComponent {
    val params by inject<Params>()
    val storageUtil by inject<StorageUtil>()

    override suspend fun CoroutineScope.onOkAsync(input: String, dialog: DialogInterface) {
        input
            .toInt()
            .takeIf { it >= 5 }
            ?.let {
                params.autoSaveTime.set(it)
                storageUtil.storeAutoSaveTime(it)
            }
            ?: throw Exception()

        updateAutosaveTimeButtonChannel.send(Unit)
    }
}