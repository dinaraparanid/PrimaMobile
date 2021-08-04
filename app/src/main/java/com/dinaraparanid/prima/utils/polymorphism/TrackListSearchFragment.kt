package com.dinaraparanid.prima.utils.polymorphism

import android.view.Gravity
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.StorageUtil

/**
 * Track [ListFragment] with search functions
 */

abstract class TrackListSearchFragment<T : Track, VH : RecyclerView.ViewHolder> :
    ListFragment<T, VH>() {
    /** Search  */
    enum class SearchOrder {
        TITLE, ARTIST, ALBUM
    }

    protected val searchOrder: MutableList<SearchOrder> by lazy {
        StorageUtil(requireContext())
            .loadTrackSearchOrder()
            ?.toMutableList()
            ?: mutableListOf(
                SearchOrder.TITLE,
                SearchOrder.ARTIST,
                SearchOrder.ALBUM
            )
    }

    /**
     * Shows menu with search params to select
     */

    protected fun selectSearch(): Boolean =
        PopupMenu(requireContext(), (requireActivity() as MainActivity).toolbar).run {
            menuInflater.inflate(R.menu.menu_track_search, menu)

            gravity = Gravity.END

            menu.findItem(R.id.search_by_title).isChecked = SearchOrder.TITLE in searchOrder
            menu.findItem(R.id.search_by_artist).isChecked = SearchOrder.ARTIST in searchOrder
            menu.findItem(R.id.search_by_album).isChecked = SearchOrder.ALBUM in searchOrder

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.search_by_title -> when (SearchOrder.TITLE) {
                        in searchOrder ->
                            if (searchOrder.size > 1) searchOrder.remove(SearchOrder.TITLE)

                        else -> searchOrder.add(SearchOrder.TITLE)
                    }

                    R.id.search_by_artist -> when (SearchOrder.ARTIST) {
                        in searchOrder ->
                            if (searchOrder.size > 1) searchOrder.remove(SearchOrder.ARTIST)
                        else -> searchOrder.add(SearchOrder.ARTIST)
                    }

                    else -> when (SearchOrder.ALBUM) {
                        in searchOrder ->
                            if (searchOrder.size > 1) searchOrder.remove(SearchOrder.ALBUM)
                        else -> searchOrder.add(SearchOrder.ALBUM)
                    }
                }

                StorageUtil(requireContext()).storeTrackSearchOrder(searchOrder)
                true
            }

            show()
            true
        }
}