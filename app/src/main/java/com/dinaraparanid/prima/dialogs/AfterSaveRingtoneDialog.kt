package com.dinaraparanid.prima.dialogs

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
        setContentView(
            DataBindingUtil
                .inflate<DialogAfterSaveRingtoneBinding>(
                    layoutInflater,
                    R.layout.dialog_after_save_ringtone,
                    null, false
                )
                .apply {
                    viewModel = ViewModel()
                    buttonMakeDefault.setOnClickListener { closeAndSendResult(R.id.button_make_default) }
                    buttonChooseContact.setOnClickListener { closeAndSendResult(R.id.button_choose_contact) }
                    buttonDoNothing.setOnClickListener { closeAndSendResult(R.id.button_do_nothing) }
                    executePendingBindings()
                }.root
        )

        this.response = response
    }

    private fun closeAndSendResult(clickedButtonId: Int) {
        response.arg1 = clickedButtonId
        response.sendToTarget()
        dismiss()
    }
}
