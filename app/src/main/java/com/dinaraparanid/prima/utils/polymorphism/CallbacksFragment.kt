package com.dinaraparanid.prima.utils.polymorphism

import android.content.Context
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.MainActivity

/**
 * [AbstractFragment] with callbacks
 */

abstract class CallbacksFragment<B : ViewDataBinding> : AbstractFragment<B, MainActivity>() {
    interface Callbacks

    /** Callbacks to call when user clicks on item */

    protected var callbacker: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacker = context as Callbacks?
    }

    override fun onDetach() {
        callbacker = null
        super.onDetach()
    }
}