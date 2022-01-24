package com.dinaraparanid.prima.utils.polymorphism

import android.widget.TextView
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import java.io.Serializable

/** Ancestor for all fragments with [RecyclerView] */

abstract class ListFragment<Act, T, A, VH, B> :
    CallbacksFragment<B, Act>(),
    AsyncContext
        where Act: AbstractActivity,
              T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : RecyclerView.Adapter<VH>,
              B : ViewDataBinding {
    /** [RecyclerView.Adapter] for every fragment */
    protected abstract var _adapter: A?
    protected inline val adapter get() = _adapter!!

    /**
     * [ViewModel] for every fragment.
     * Mainly used to call coroutines and remember some data
     */

    abstract val viewModel: ViewModel

    /** [TextView] that shows when there are no entities */
    protected abstract var emptyTextView: TextView?

    /** [RecyclerView] for every fragment */
    protected var recyclerView: RecyclerView? = null

    final override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    override fun onDestroyView() {
        super.onDestroyView()
        emptyTextView = null
        recyclerView = null
        _adapter = null
    }

    /**
     * Sets [emptyTextView] visibility.
     * If [src] is empty [TextView.VISIBLE],
     * else [TextView.INVISIBLE]
     *
     * @param src entities to show in fragment (if there are any)
     */

    protected fun setEmptyTextViewVisibility(src: List<T>) {
        emptyTextView!!.visibility = if (src.isEmpty()) TextView.VISIBLE else TextView.INVISIBLE
    }

    /** Initializes [_adapter]. It should be done in [onCreateView] */
    protected abstract fun initAdapter()
}