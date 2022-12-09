package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import android.widget.Toast
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

/** [InputDialogUIHandler] for CheckHiddenPasswordDialog */

class CheckHiddenPasswordUIHandler(
    private val passwordHash: Int,
    private val showHiddenFragmentChannel: Channel<Unit>
) : InputDialogUIHandler {
    override suspend fun CoroutineScope.onOkAsync(input: String, dialog: DialogInterface) =
        when (input.hashCode()) {
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