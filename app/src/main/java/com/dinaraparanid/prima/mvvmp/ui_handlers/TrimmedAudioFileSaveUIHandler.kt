package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.mvvmp.view.dialogs.TrimmedAudioFileSaveDialogFragment
import kotlinx.coroutines.channels.Channel

/** [UIHandler] for TrimmedAudioFileSaveDialog */

class TrimmedAudioFileSaveUIHandler : UIHandler {
    suspend inline fun sendFileDataAndCloseDialog(
        fileData: TrimmedAudioFileSaveDialogFragment.TrimmedAudioFileData,
        fileDataChannel: Channel<TrimmedAudioFileSaveDialogFragment.TrimmedAudioFileData>,
        dialog: DialogInterface
    ) {
        fileDataChannel.send(fileData)
        dialog.dismiss()
    }

    fun closeDialog(dialog: DialogInterface) = dialog.dismiss()
}