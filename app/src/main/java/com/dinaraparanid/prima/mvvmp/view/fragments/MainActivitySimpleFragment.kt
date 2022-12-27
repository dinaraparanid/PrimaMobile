package com.dinaraparanid.prima.mvvmp.view.fragments

import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivitySimpleFragment<P, VM, H, B> :
    ObservableFragment<P, VM, H, B, MainActivity>(),
    MainActivityFragment,
    MenuProviderFragment
        where P : BasePresenter,
              VM : ObservableViewModel<P>,
              H : UIHandler,
              B : ViewDataBinding {
    final override val menuProvider = defaultMenuProvider

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
    }

    final override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }
}