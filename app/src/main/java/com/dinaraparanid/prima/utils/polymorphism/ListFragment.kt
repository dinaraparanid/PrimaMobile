package com.dinaraparanid.prima.utils.polymorphism

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import kotlinx.coroutines.Deferred
import java.io.Serializable

abstract class ListFragment<T : Serializable, VH : RecyclerView.ViewHolder> :
    Fragment(),
    SearchView.OnQueryTextListener,
    ContentUpdatable<List<T>>,
    FilterFragment<T>,
    RecyclerViewUp,
    Loader<List<T>> {
    interface Callbacks

    protected abstract var adapter: RecyclerView.Adapter<VH>?
    protected abstract val viewModel: ViewModel

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var mainLabelOldText: String
    protected lateinit var mainLabelCurText: String
    protected lateinit var titleDefault: String

    var genFunc: (suspend () -> Deferred<List<T>>)? = null
    protected var callbacks: Callbacks? = null
    protected val itemList: MutableList<T> = mutableListOf()
    protected val itemListSearch: MutableList<T> = mutableListOf()

    protected companion object {
        const val MAIN_LABEL_OLD_TEXT_KEY: String = "main_label_old_text"
        const val MAIN_LABEL_CUR_TEXT_KEY: String = "main_label_cur_text"
    }

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

    override fun onStop() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelOldText
        super.onStop()
    }

    override fun onResume() {
        (requireActivity() as MainActivity).run {
            mainLabel.text = mainLabelCurText
            currentFragment = this@ListFragment
        }

        super.onResume()
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