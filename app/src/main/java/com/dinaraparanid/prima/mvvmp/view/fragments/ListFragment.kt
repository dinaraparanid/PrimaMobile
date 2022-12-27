package com.dinaraparanid.prima.mvvmp.view.fragments

import android.os.Parcelable
import androidx.annotation.UiThread
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import kotlinx.coroutines.CoroutineScope

/** Ancestor for all fragments with [RecyclerView] */

abstract class ListFragment<P, VM, H, B, Act, T, Adp, VH> :
    ObservableFragment<P, VM, H, B, Act>(),
    AsyncContext
        where P : BasePresenter,
              VM : ObservableViewModel<P>,
              H : UIHandler,
              B : ViewDataBinding,
              Act : AbstractActivity,
              T : Parcelable,
              VH : RecyclerView.ViewHolder,
              Adp : RecyclerView.Adapter<VH> {
    protected abstract var adapter: Adp

    final override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    /**
     * Initializes [adapter].
     * It should be called in [ObservableFragment.onCreateView]
     */

    @UiThread
    protected abstract fun initAdapter()
}