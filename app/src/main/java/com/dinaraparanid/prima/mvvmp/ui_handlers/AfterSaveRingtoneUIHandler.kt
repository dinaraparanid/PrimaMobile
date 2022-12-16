package com.dinaraparanid.prima.mvvmp.ui_handlers

import com.dinaraparanid.prima.mvvmp.view.dialogs.AfterSaveRingtoneDialogFragment
import kotlinx.coroutines.channels.Channel

/** [UIHandler] for [AfterSaveRingtoneDialogFragment] */

class AfterSaveRingtoneUIHandler : UIHandler {
    suspend inline fun closeDialogAndSendMakeDefault(
        dialog: AfterSaveRingtoneDialogFragment,
        channel: Channel<AfterSaveRingtoneDialogFragment.AfterSaveRingtoneTarget>
    ) {
        channel.send(AfterSaveRingtoneDialogFragment.AfterSaveRingtoneTarget.MAKE_DEFAULT)
        dialog.dismiss()
    }

    suspend inline fun closeDialogAndSendSetToContact(
        dialog: AfterSaveRingtoneDialogFragment,
        channel: Channel<AfterSaveRingtoneDialogFragment.AfterSaveRingtoneTarget>
    ) {
        channel.send(AfterSaveRingtoneDialogFragment.AfterSaveRingtoneTarget.SET_TO_CONTACT)
        dialog.dismiss()
    }

    suspend inline fun closeDialogAndSendIgnore(
        dialog: AfterSaveRingtoneDialogFragment,
        channel: Channel<AfterSaveRingtoneDialogFragment.AfterSaveRingtoneTarget>
    ) {
        channel.send(AfterSaveRingtoneDialogFragment.AfterSaveRingtoneTarget.IGNORE)
        dialog.dismiss()
    }
}