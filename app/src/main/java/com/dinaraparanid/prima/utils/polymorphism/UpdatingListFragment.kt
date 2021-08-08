package com.dinaraparanid.prima.utils.polymorphism

import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.Serializable

/**
 * [ListFragment] with swipe fresh layout
 * (to update its [itemList])
 */

abstract class UpdatingListFragment<T : Serializable, VH : RecyclerView.ViewHolder> :
    ListFragment<T, VH>() {
    protected abstract var updater: SwipeRefreshLayout

    override fun onPause() {
        super.onPause()
        updater.isEnabled = false
        updater.clearAnimation()
        updater.clearDisappearingChildren()
        updater.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        updater.isEnabled = true
    }

    /**
     * Like [UIUpdatable.updateUI] but src is [itemList]
     */

    internal fun updateUI() = updateUI(itemList)
}