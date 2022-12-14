package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import com.dinaraparanid.prima.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import java.io.File

class NewFolderUIHandler : InputDialogUIHandler<NewFolderUIHandler.NewFolderUIHandlerArgs> {
    data class NewFolderUIHandlerArgs(
        val context: Context,
        val path: String,
        val updateFragmentUIChannel: Channel<Unit>
    ) : InputDialogUIHandler.Args

    override suspend fun CoroutineScope.onOkAsync(
        input: String,
        dialog: DialogInterface,
        args: NewFolderUIHandlerArgs
    ) {
        Toast.makeText(
            args.context,
            when {
                File("${args.path}/$input").mkdir() -> R.string.folder_create_success
                else -> R.string.folder_create_error
            },
            Toast.LENGTH_LONG
        ).show()

        args.updateFragmentUIChannel.send(Unit)
    }
}