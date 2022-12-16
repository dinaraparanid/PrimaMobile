package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.mvvmp.view.dialogs.TrimmedAudioFileSaveDialog
import kotlinx.coroutines.channels.Channel

/** [UIHandler] for TrimmedAudioFileSaveDialog */

class TrimmedAudioFileSaveUIHandler : UIHandler {
    suspend inline fun sendFileDataAndCloseDialog(
        fileData: TrimmedAudioFileSaveDialog.TrimmedAudioFileData,
        fileDataChannel: Channel<TrimmedAudioFileSaveDialog.TrimmedAudioFileData>,
        dialog: DialogInterface
    ) {
        fileDataChannel.send(fileData)
        dialog.dismiss()
    }

    fun closeDialog(dialog: DialogInterface) = dialog.dismiss()
}