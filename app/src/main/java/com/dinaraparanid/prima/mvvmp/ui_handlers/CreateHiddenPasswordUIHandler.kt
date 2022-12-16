package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.mvvmp.view.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.utils.StorageUtil
import de.nycode.bcrypt.hash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent

/** [InputDialogUIHandler] for [CreateHiddenPasswordDialog] */

class CreateHiddenPasswordUIHandler(private val storageUtil: StorageUtil) :
    InputDialogUIHandler<CreateHiddenPasswordUIHandler.CreateHiddenPasswordUIHandlerArgs> {
    data class CreateHiddenPasswordUIHandlerArgs(
        val target: CreateHiddenPasswordDialog.Target,
        val showHiddenFragmentChannel: Channel<Unit>
    ) : InputDialogUIHandler.Args

    override suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: CreateHiddenPasswordUIHandlerArgs
    ) {
        storageUtil.storeHiddenPassword(hash(input).hashCode())
        if (args.target == CreateHiddenPasswordDialog.Target.CREATE)
            args.showHiddenFragmentChannel.send(Unit)
    }
}