package com.dinaraparanid.prima.utils.polymorphism

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

abstract class ListFragment<T : Serializable, VH : RecyclerView.ViewHolder> :
    AbstractFragment(),
    SearchView.OnQueryTextListener,
    ContentUpdatable<List<T>>,
    FilterFragment<T>,
    RecyclerViewUp,
    Loader<List<T>> {
    interface Callbacks

    protected abstract var adapter: RecyclerView.Adapter<VH>?
    protected abstract val viewModel: ViewModel

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var titleDefault: String

    var genFunc: (() -> List<T>)? = null
    protected var callbacks: Callbacks? = null
    protected val itemList: MutableList<T> = mutableListOf()
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
        val filteredModelList = filter(
            itemList,
            query ?: ""
        )

        itemListSearch.clear()
        itemListSearch.addAll(filteredModelList)
        adapter?.notifyDataSetChanged()
        updateUI(itemListSearch)

        recyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override val loaderContent: List<T> get() = itemList

    override fun up() {
        recyclerView.layoutParams =
            (recyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomMargin = 200
            }
    }
}