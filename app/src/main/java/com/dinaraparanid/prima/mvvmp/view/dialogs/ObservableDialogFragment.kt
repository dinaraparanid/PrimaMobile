package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view.ObservableView
import com.dinaraparanid.prima.mvvmp.view.startHandleUIStatesChanges
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel

/** [ObservableView] for [DialogFragment]s */

abstract class ObservableDialogFragment<
        P : BasePresenter,
        VM : ObservableViewModel<P>,
        H : UIHandler,
        B : ViewDataBinding
> : DialogFragment(), ObservableView<P, VM, H, B> {
    final override lateinit var binding: B
    protected abstract val dialogBinding: B

    protected open val dialogView
        get() = Dialog(requireContext()).apply {
            setContentView(binding.root)
            setDialogProperties()
            startHandleUIStatesChanges(viewLifecycleOwner)
        }

    protected open fun Dialog.setDialogProperties() = Unit

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = dialogBinding
        return dialogView
    }
}