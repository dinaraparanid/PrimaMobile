package com.dinaraparanid.prima.trimmer

import android.app.Activity
import android.app.Dialog
import android.os.Message
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogAfterSaveRingtoneBinding
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

internal class AfterSaveRingtoneDialog(activity: Activity, response: Message) :
    Dialog(activity) {
    private val response: Message

    init {
        DataBindingUtil.setContentView<DialogAfterSaveRingtoneBinding>(
            activity, R.layout.dialog_after_save_ringtone
        ).apply {
            viewModel = ViewModel()
            buttonMakeDefault.setOnClickListener { closeAndSendResult(R.id.button_make_default) }
            buttonChooseContact.setOnClickListener { closeAndSendResult(R.id.button_choose_contact) }
            buttonDoNothing.setOnClickListener { closeAndSendResult(R.id.button_do_nothing) }
            executePendingBindings()
        }

        this.response = response
    }

    private fun closeAndSendResult(clickedButtonId: Int) {
        response.arg1 = clickedButtonId
        response.sendToTarget()
        dismiss()
    }
}
