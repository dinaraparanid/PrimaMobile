package com.dinaraparanid.prima.utils.polymorphism

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import java.io.Serializable

/**
 * Ancestor for all fragments with [RecyclerView]
 */

abstract class ListFragment<T : Serializable, VH : RecyclerView.ViewHolder> :
    AbstractFragment(),
    SearchView.OnQueryTextListener,
    UIUpdatable<List<T>>,
    FilterFragment<T>,
    Rising,
    Loader<List<T>> {
    interface Callbacks

    /**
     * [RecyclerView.Adapter] for every fragment
     */

    protected abstract var adapter: RecyclerView.Adapter<VH>?

    /**
     * [ViewModel] for every fragment.
     * Mainly used to call coroutines and remember some data
     */

    protected abstract val viewModel: ViewModel

    /** [RecyclerView] for every fragment */

    protected lateinit var recyclerView: RecyclerView

    /** Default title if there weren't any in params */

    protected lateinit var titleDefault: String

    /** Callbacks to call when user clicks on item */

    protected var callbacks: Callbacks? = null

    /** Item list for every fragment */

    protected val itemList: MutableList<T> = mutableListOf()

    /** Item list to use in search operations */

    protected val itemListSearch: MutableList<T> = mutableListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateUI(itemListSearch)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null && query.isNotEmpty()) {
            val filteredModelList = filter(
                itemList,
                query
            )

            itemListSearch.clear()
            itemListSearch.addAll(filteredModelList)
            adapter?.notifyDataSetChanged()
            updateUI(itemListSearch)

            recyclerView.scrollToPosition(0)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override val loaderContent: List<T> get() = itemList

    override fun up() {
        if (!(requireActivity() as MainActivity).upped)
            recyclerView.layoutParams =
                (recyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = 200
                }
    }
}