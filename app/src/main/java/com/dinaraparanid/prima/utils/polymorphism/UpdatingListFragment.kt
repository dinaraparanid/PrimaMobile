package com.dinaraparanid.prima.utils.polymorphism

import androidx.appcompat.widget.SearchView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import kotlinx.coroutines.sync.Mutex
import java.io.Serializable
import java.util.Collections

/**
 * [ListFragment] with swipe fresh layout
 * (to update its [itemList])
 */

abstract class UpdatingListFragment<Act, T, A, VH, B> :
    ListFragment<Act, T, A, VH, B>(),
    SearchView.OnQueryTextListener,
    UIUpdatable<List<T>>,
    FilterFragment<T>,
    Loader<List<T>>
        where Act: AbstractActivity,
              T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH>,
              B : ViewDataBinding {

    /** Item list for every fragment */
    protected val itemList: MutableList<T> = Collections.synchronizedList(mutableListOf())

    /** Item list to use in search operations */
    protected val itemListSearch: MutableList<T> = Collections.synchronizedList(mutableListOf())

    /** Swipe refresh layout to update [itemList] */
    protected abstract var updater: SwipeRefreshLayout?

    final override val mutex = Mutex()

    final override fun onPause() {
        super.onPause()
        updater!!.clearAnimation()
        updater!!.clearDisappearingChildren()
        updater!!.clearFocus()
        updater!!.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updater = null
    }

    override fun onDestroy() {
        super.onDestroy()
        itemList.clear()
        itemListSearch.clear()
    }

    override fun onResume() {
        super.onResume()
        updater!!.isEnabled = true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null && query.isNotEmpty()) {
            val filteredModelList = filter(
                itemList,
                query
            )

            runOnUIThread {
                itemListSearch.clear()
                itemListSearch.addAll(filteredModelList)
                updateUI(itemListSearch, isLocking = true)
            }
        }
        return true
    }

    final override fun onLowMemory() {
        super.onLowMemory()
        itemListSearch.clear()
    }

    final override fun onQueryTextSubmit(query: String?): Boolean = false

    final override val loaderContent: List<T> get() = itemList

    /** Like [UIUpdatable.updateUI] but src is [itemList] */
    internal suspend fun updateUI(isLocking: Boolean) = updateUI(itemList, isLocking)

    /**
     * Loads content with [loadAsync]
     * and updates UI with [updateUI]
     */

    internal fun updateUIOnChangeContentAsync() = runOnUIThread {
        val task = loadAsync()
        val progress = createAndShowAwaitDialog(requireContext(), false)

        task.join()
        progress.dismiss()
        updateUI(isLocking = true)
    }
}