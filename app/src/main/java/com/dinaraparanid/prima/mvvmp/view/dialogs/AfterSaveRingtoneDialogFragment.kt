package com.dinaraparanid.prima.mvvmp.view.dialogs

import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogAfterSaveRingtoneBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.AfterSaveRingtoneUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.AfterSaveRingtoneViewModel
import kotlinx.coroutines.channels.Channel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

/**
 * Shown when ringtone is saved.
 * User chooses what to do with ringtone:
 * 1) Make default ringtone
 * 2) Set ringtone to contact
 * 3) Do nothing and close dialog
 */

class AfterSaveRingtoneDialogFragment(private val channel: Channel<AfterSaveRingtoneTarget>) :
    ObservableDialogFragment<
            BasePresenter,
            AfterSaveRingtoneViewModel,
            AfterSaveRingtoneUIHandler,
            DialogAfterSaveRingtoneBinding
            >() {
    /** Next action after ringtone being saved */
    enum class AfterSaveRingtoneTarget {
        MAKE_DEFAULT, SET_TO_CONTACT, IGNORE
    }

    override val uiHandler by inject<AfterSaveRingtoneUIHandler>()
    override val viewModel by viewModel<AfterSaveRingtoneViewModel>()

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(uiHandler, viewModel.isMakeDefaultButtonPressedState) {
                closeDialogAndSendMakeDefault(this@AfterSaveRingtoneDialogFragment, channel)
                viewModel.finishSettingDefaultRingtone()
            },
            StateChangedCallback(uiHandler, viewModel.isChooseContactButtonPressedState) {
                closeDialogAndSendSetToContact(this@AfterSaveRingtoneDialogFragment, channel)
                viewModel.finishSettingContactRingtone()
            },
            StateChangedCallback(uiHandler, viewModel.isDoNothingButtonPressedState) {
                closeDialogAndSendIgnore(this@AfterSaveRingtoneDialogFragment, channel)
                viewModel.finishIgnoreResultingRingtone()
            }
        )
    }

    override val dialogBinding
        get() = DataBindingUtil
            .inflate<DialogAfterSaveRingtoneBinding>(
                layoutInflater,
                R.layout.dialog_after_save_ringtone,
                null, false
            )
            .apply {
                viewModel = this@AfterSaveRingtoneDialogFragment.viewModel
                executePendingBindings()
            }
}
