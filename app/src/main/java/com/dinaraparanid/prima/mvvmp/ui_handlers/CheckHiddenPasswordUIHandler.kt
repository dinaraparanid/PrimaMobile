package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import android.widget.Toast
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

/** [InputDialogUIHandler] for CheckHiddenPasswordDialog */

class CheckHiddenPasswordUIHandler : InputDialogUIHandler<CheckHiddenPasswordUIHandler.CheckHiddenPasswordUIHandlerArgs> {
    data class CheckHiddenPasswordUIHandlerArgs(
        val passwordHash: Int,
        val showHiddenFragmentChannel: Channel<Unit>
    ) : InputDialogUIHandler.Args

    override suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: CheckHiddenPasswordUIHandlerArgs
    ) = when (input.hashCode()) {
        args.passwordHash -> args.showHiddenFragmentChannel.send(Unit)

        else -> {
            dialog.dismiss()

            Toast.makeText(
                Params.getInstanceSynchronized().application.unchecked,
                R.string.incorrect_password,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}