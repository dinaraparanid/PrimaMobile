package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogCurrentReleaseBinding
import com.dinaraparanid.prima.databinding.DialogNewReleaseBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.PrimaReleasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.PrimaReleaseUIHandler
import com.dinaraparanid.prima.mvvmp.view.dialogs.PrimaReleaseDialogFragment.Target
import com.dinaraparanid.prima.mvvmp.view_models.PrimaReleaseViewModel
import com.dinaraparanid.prima.utils.web.github.ReleaseInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * [Dialog] that shows when the new version is released.
 * Requests user to update or just cancels it.
 *
 * @param releaseInfo new [ReleaseInfo]
 * @param target [Target] the dialog's creation's reason
 */

class PrimaReleaseDialogFragment(releaseInfo: ReleaseInfo, private val target: Target) :
    ObservableDialogFragment<
            PrimaReleasePresenter,
            PrimaReleaseViewModel,
            PrimaReleaseUIHandler,
            ViewDataBinding
            >() {

    /** The reason why is dialog called */
    enum class Target { NEW, CURRENT }

    override val uiHandler by inject<PrimaReleaseUIHandler>()

    override val viewModel by viewModel<PrimaReleaseViewModel> {
        parametersOf(releaseInfo, target)
    }

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(uiHandler, viewModel.isUpdateButtonPressedState) {
                sendToDownload(requireContext())
                viewModel.finishUpdateButtonPressedEvent()
                dismiss()
            },
            StateChangedCallback(uiHandler, viewModel.isUpdateLaterButtonPressedState) {
                viewModel.finishUpdateLaterButtonPressedEvent()
                dismiss()
            }
        )
    }

    override fun Dialog.setDialogProperties() = setCancelable(true)

    override val dialogBinding
        get() = when (target) {
            Target.NEW -> DataBindingUtil
                .inflate<DialogNewReleaseBinding>(
                    layoutInflater,
                    R.layout.dialog_new_release,
                    null, false
                )
                .apply {
                    viewModel = this@PrimaReleaseDialogFragment.viewModel
                    executePendingBindings()
                }

            Target.CURRENT -> DataBindingUtil
                .inflate<DialogCurrentReleaseBinding>(
                    layoutInflater,
                    R.layout.dialog_current_release,
                    null, false
                )
                .apply {
                    viewModel = this@PrimaReleaseDialogFragment.viewModel
                    executePendingBindings()
                }
        }
}