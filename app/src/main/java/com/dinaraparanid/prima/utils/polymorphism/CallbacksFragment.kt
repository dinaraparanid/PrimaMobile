package com.dinaraparanid.prima.utils.polymorphism

import android.content.Context
import androidx.databinding.ViewDataBinding

/** [AbstractFragment] with callbacks */

abstract class CallbacksFragment<B, A> : AbstractFragment<B, A>()
        where B : ViewDataBinding,
              A : AbstractActivity {
    interface Callbacks

    /** Callbacks to call when user clicks on item */

    protected var callbacker: Callbacks? = null

    final override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacker = context as Callbacks?
    }

    final override fun onDetach() {
        callbacker = null
        super.onDetach()
    }
}