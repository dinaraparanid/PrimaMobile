package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogAfterSaveRingtoneBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.AfterSaveRingtonePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.AfterSaveRingtoneUIHandler
import com.dinaraparanid.prima.mvvmp.view.ObservableView
import com.dinaraparanid.prima.mvvmp.view.handleUIStatesChanges
import com.dinaraparanid.prima.mvvmp.view_models.AfterSaveRingtoneViewModel
import kotlinx.coroutines.channels.Channel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Shown when ringtone is saved.
 * User chooses what to do with ringtone:
 * 1) Make default ringtone
 * 2) Set ringtone to contact
 * 3) Do nothing and close dialog
 */

@Suppress("Reformat")
class AfterSaveRingtoneDialog(
    fragment: Fragment,
    private val channel: Channel<AfterSaveRingtoneTarget>
) : DialogFragment(),
    ObservableView<AfterSaveRingtonePresenter, AfterSaveRingtoneViewModel, AfterSaveRingtoneUIHandler, DialogAfterSaveRingtoneBinding>,
    KoinComponent {

    /** Next action after ringtone being saved */
    enum class AfterSaveRingtoneTarget {
        MAKE_DEFAULT, SET_TO_CONTACT, IGNORE
    }

    override val uiHandler by inject<AfterSaveRingtoneUIHandler>()
    override lateinit var binding: DialogAfterSaveRingtoneBinding

    override val viewModel by lazy {
        fragment.requireActivity().getViewModel<AfterSaveRingtoneViewModel>()
    }

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(uiHandler, viewModel.isMakeDefaultButtonPressedState) {
                closeDialogAndSendMakeDefault(this@AfterSaveRingtoneDialog, channel)
                viewModel.finishSettingDefaultRingtone()
            },
            StateChangedCallback(uiHandler, viewModel.isChooseContactButtonPressedState) {
                closeDialogAndSendSetToContact(this@AfterSaveRingtoneDialog, channel)
                viewModel.finishSettingContactRingtone()
            },
            StateChangedCallback(uiHandler, viewModel.isDoNothingButtonPressedState) {
                closeDialogAndSendIgnore(this@AfterSaveRingtoneDialog, channel)
                viewModel.finishIgnoreResultingRingtone()
            }
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DataBindingUtil
            .inflate<DialogAfterSaveRingtoneBinding>(
                layoutInflater,
                R.layout.dialog_after_save_ringtone,
                null, false
            )
            .apply {
                viewModel = this@AfterSaveRingtoneDialog.viewModel
                executePendingBindings()
            }

        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            handleUIStatesChanges(binding.lifecycleOwner!!)
        }
    }
}
