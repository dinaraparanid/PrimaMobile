package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import android.widget.Toast
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import kotlinx.coroutines.channels.Channel

/** [InputDialogUIHandler] for CheckHiddenPasswordDialog */

class CheckHiddenPasswordUIHandler :
    InputDialogUIHandler<CheckHiddenPasswordUIHandler.Args> {
    data class Args(val passwordHash: Int, val showHiddenFragmentChannel: Channel<Unit>) :
        InputDialogUIHandler.Args

    override suspend fun Args.onOkAsync(
        input: String,
        dialog: DialogInterface
    ) = when (input.hashCode()) {
        passwordHash -> showHiddenFragmentChannel.send(Unit)

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