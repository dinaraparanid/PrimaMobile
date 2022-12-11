package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.mvvmp.view.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.utils.StorageUtil
import de.nycode.bcrypt.hash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** [InputDialogUIHandler] for [CreateHiddenPasswordDialog] */

class CreateHiddenPasswordUIHandler(
    private val target: CreateHiddenPasswordDialog.Target,
    private val showHiddenFragmentChannel: Channel<Unit>
) : InputDialogUIHandler, KoinComponent {
    private val storageUtil by inject<StorageUtil>()

    override suspend fun CoroutineScope.onOkAsync(input: String, dialog: DialogInterface) {
        storageUtil.storeHiddenPassword(hash(input).hashCode())
        if (target == CreateHiddenPasswordDialog.Target.CREATE)
            showHiddenFragmentChannel.send(Unit)
    }
}