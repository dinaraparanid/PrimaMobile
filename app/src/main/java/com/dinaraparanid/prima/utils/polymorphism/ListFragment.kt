package com.dinaraparanid.prima.utils.polymorphism

import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import java.io.Serializable

/**
 * Ancestor for all fragments with [RecyclerView]
 */

abstract class ListFragment<T, A, VH, B> :
    CallbacksFragment<B>(),
    Rising
        where T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : RecyclerView.Adapter<VH>,
              B : ViewDataBinding{
    /**
     * [RecyclerView.Adapter] for every fragment
     */

    protected abstract var adapter: A?

    /**
     * [ViewModel] for every fragment.
     * Mainly used to call coroutines and remember some data
     */

    protected abstract val viewModel: ViewModel

    /**
     * [TextView] that shows when there are no entities
     */

    protected abstract var emptyTextView: TextView

    /** [RecyclerView] for every fragment */

    protected lateinit var recyclerView: RecyclerView

    /** Default title if there weren't any in params */

    protected lateinit var titleDefault: String

    override fun up() {
        if (!(requireActivity() as MainActivity).upped)
            recyclerView.layoutParams =
                (recyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = (requireActivity() as MainActivity).playingToolbarHeight
                }
    }

    /**
     * Sets [emptyTextView] visibility.
     * If [src] is empty [TextView.VISIBLE],
     * else [TextView.INVISIBLE]
     *
     * @param src entities to show in fragment (if there are any)
     */

    protected fun setEmptyTextViewVisibility(src: List<T>) {
        emptyTextView.visibility = if (src.isEmpty()) TextView.VISIBLE else TextView.INVISIBLE
    }
}