package com.dinaraparanid.prima.mvvmp.ui_handlers

import com.dinaraparanid.prima.mvvmp.view.dialogs.AfterSaveRingtoneDialog
import kotlinx.coroutines.channels.Channel

/** [UIHandler] for [AfterSaveRingtoneDialog] */

class AfterSaveRingtoneUIHandler : UIHandler {
    suspend inline fun closeDialogAndSendMakeDefault(
        dialog: AfterSaveRingtoneDialog,
        channel: Channel<AfterSaveRingtoneDialog.AfterSaveRingtoneTarget>
    ) {
        channel.send(AfterSaveRingtoneDialog.AfterSaveRingtoneTarget.MAKE_DEFAULT)
        dialog.dismiss()
    }

    suspend inline fun closeDialogAndSendSetToContact(
        dialog: AfterSaveRingtoneDialog,
        channel: Channel<AfterSaveRingtoneDialog.AfterSaveRingtoneTarget>
    ) {
        channel.send(AfterSaveRingtoneDialog.AfterSaveRingtoneTarget.SET_TO_CONTACT)
        dialog.dismiss()
    }

    suspend inline fun closeDialogAndSendIgnore(
        dialog: AfterSaveRingtoneDialog,
        channel: Channel<AfterSaveRingtoneDialog.AfterSaveRingtoneTarget>
    ) {
        channel.send(AfterSaveRingtoneDialog.AfterSaveRingtoneTarget.IGNORE)
        dialog.dismiss()
    }
}