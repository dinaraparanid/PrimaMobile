package com.dinaraparanid.prima.mvvmp.view

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

interface ObservableView<
        P : BasePresenter,
        VM : ObservableViewModel<P>,
        H : UIHandler,
        B : ViewDataBinding
> : KoinComponent {
    val viewModel: VM
    val uiHandler: H
    var binding: B
    val stateChangesCallbacks: Array<StateChangedCallback<H>>
}

fun <P : BasePresenter, VM : ObservableViewModel<P>, H : UIHandler, B : ViewDataBinding>
        ObservableView<P, VM, H, B>.handleUIStatesChanges(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            stateChangesCallbacks.forEach { callback -> callback() }
        }
    }
}