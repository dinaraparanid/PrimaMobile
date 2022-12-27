package com.dinaraparanid.prima.mvvmp.view.fragments

import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view.ObservableView
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity

/** Ancestor [Fragment] for all app's fragments */

abstract class ObservableFragment<P, VM, H, B, A> : Fragment(), ObservableView<P, VM, H, B>
        where P : BasePresenter,
              VM : ObservableViewModel<P>,
              H : UIHandler,
              B : ViewDataBinding,
              A : AbstractActivity {
    protected inline val fragmentActivity
        @Suppress("UNCHECKED_CAST")
        get() = requireActivity() as A

    protected inline val application
        get() = requireActivity().application as MainApplication
}