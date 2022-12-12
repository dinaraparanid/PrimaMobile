package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.AlertDialog
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.GTMSetStartPlaybackPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.GTMPropertiesUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.GTMPropertiesViewModel

/** Ancestor for GTM properties dialogs */

abstract class GTMSetPropertiesDialog<
        P : GTMSetStartPlaybackPresenter,
        VM : GTMPropertiesViewModel<P>,
        H : GTMPropertiesUIHandler,
        B : ViewDataBinding
        > : ObservableDialogFragment<P, VM, H, B>() {
    final override val stateChangesCallbacks = emptyArray<StateChangedCallback<H>>()

    final override val dialogView
        get() = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                uiHandler
                    .runCatching { onOkPressed(dialog) }
                    .getOrElse {
                        uiHandler.dismissAndShowError(
                            context = requireContext(),
                            message = R.string.unknown_error,
                            dialog = dialog
                        )
                    }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
}