package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.content.Context
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity

/** [AbstractFragment] with callbacks */

abstract class CallbacksFragment<B, A> : AbstractFragment<B, A>()
        where B : ViewDataBinding,
              A : AbstractActivity {
    interface Callbacks

    /** Callbacks to call when user clicks on item */

    protected var callbacker: Callbacks? = null

    /** Initializes [callbacker] */
    final override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacker = context as Callbacks?
    }

    /** Deinitializes [callbacker] */
    final override fun onDetach() {
        callbacker = null
        super.onDetach()
    }
}