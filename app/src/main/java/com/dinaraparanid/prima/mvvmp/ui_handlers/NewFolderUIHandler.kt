package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import com.dinaraparanid.prima.R
import kotlinx.coroutines.channels.Channel
import java.io.File

/** [InputDialogUIHandler] for NewFolderDialog */

class NewFolderUIHandler : InputDialogUIHandler<NewFolderUIHandler.Args> {
    data class Args(
        val context: Context,
        val path: String,
        val updateFragmentUIChannel: Channel<Unit>
    ) : InputDialogUIHandler.Args

    override suspend fun Args.onOkAsync(
        input: String,
        dialog: DialogInterface,
    ) {
        Toast.makeText(
            context,
            when {
                File("$path/$input").mkdir() -> R.string.folder_create_success
                else -> R.string.folder_create_error
            },
            Toast.LENGTH_LONG
        ).show()

        updateFragmentUIChannel.send(Unit)
    }
}