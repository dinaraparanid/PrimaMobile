package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.mvvmp.view.dialogs.CreateHiddenPasswordDialogFragment
import com.dinaraparanid.prima.utils.StorageUtil
import de.nycode.bcrypt.hash
import kotlinx.coroutines.channels.Channel

/** [InputDialogUIHandler] for [CreateHiddenPasswordDialogFragment] */

class CreateHiddenPasswordUIHandler(private val storageUtil: StorageUtil) :
    InputDialogUIHandler<CreateHiddenPasswordUIHandler.CreateHiddenPasswordUIHandlerArgs> {
    data class CreateHiddenPasswordUIHandlerArgs(
        val target: CreateHiddenPasswordDialogFragment.Target,
        val showHiddenFragmentChannel: Channel<Unit>
    ) : InputDialogUIHandler.Args

    override suspend fun CreateHiddenPasswordUIHandlerArgs.onOkAsync(
        input: String,
        dialog: DialogInterface
    ) {
        storageUtil.storeHiddenPassword(hash(input).hashCode())
        if (target == CreateHiddenPasswordDialogFragment.Target.CREATE)
            showHiddenFragmentChannel.send(Unit)
    }
}