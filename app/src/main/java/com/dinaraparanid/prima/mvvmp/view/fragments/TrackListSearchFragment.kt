package com.dinaraparanid.prima.mvvmp.view.fragments

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.mvvmp.presenters.MainActivityListPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.inject

/** Track [MainActivityUpdatingListFragment] with search functions */

abstract class TrackListSearchFragment<P, VM, H, B, T, A, VH> :
    MainActivityUpdatingListFragment<P, VM, H, B, T, A, VH>()
        where P : MainActivityListPresenter,
              VM : ObservableViewModel<P>,
              H : UIHandler,
              B : ViewDataBinding,
              T : Track,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH> {
    private val params by inject<Params>()

    enum class SearchParams {
        TITLE, ARTIST, ALBUM
    }

    protected abstract val showSelectSearchParamsMenuChannel: Channel<MutableList<SearchParams>>

    private var searchParams: MutableList<SearchParams>? = null

    private suspend inline fun loadTrackSearchParamsAsync() =
        StorageUtil
            .getInstanceAsyncSynchronized()
            .loadTrackSearchParams()
            ?.toMutableList()

    private suspend inline fun initSearchParams() {
        searchParams = loadTrackSearchParamsAsync() ?: mutableListOf(
            SearchParams.TITLE,
            SearchParams.ARTIST,
            SearchParams.ALBUM
        )
    }

    private fun matchesParam(lowerCaseQuery: String, searchParam: SearchParams, property: String) =
        when (searchParam) {
            in searchParams!! -> lowerCaseQuery in property.lowercase()
            else -> false
        }

    private fun T.matchesTitle(lowerCaseQuery: String) = matchesParam(
        lowerCaseQuery,
        searchParam = SearchParams.TITLE,
        property = title
    )

    private fun T.matchesArtist(lowerCaseQuery: String) = matchesParam(
        lowerCaseQuery,
        searchParam = SearchParams.ARTIST,
        property = artist
    )

    private fun T.matchesAlbum(lowerCaseQuery: String) = matchesParam(
        lowerCaseQuery,
        searchParam = SearchParams.ALBUM,
        property = album
    )

    final override fun filter(query: String, models: Collection<T>): List<T> {
        val lowerCaseQuery = query.lowercase()

        return models.filter { track ->
            val titleMatch = track.matchesTitle(lowerCaseQuery)
            val artistMatch = track.matchesArtist(lowerCaseQuery)
            val albumMatch = track.matchesAlbum(lowerCaseQuery)
            titleMatch || artistMatch || albumMatch
        }
    }

    final override suspend fun filterAsync(query: String, models: Collection<T>) = coroutineScope {
        initSearchParams()
        super.filterAsync(query, models)
    }

    protected suspend fun sendSearchParams() =
        showSelectSearchParamsMenuChannel.send(searchParams!!)

    protected fun onShuffleButtonPressed() = runOnUIThread {
        adapter.setCurrentList(itemList.shuffled())
    }

    // ------------------------------- Track Order -------------------------------

    private inline val trackOrderLabel
        get() = "${
            resources.getString(
                when (params.tracksOrder.first) {
                    Params.TracksOrder.TITLE -> R.string.by_title
                    Params.TracksOrder.ARTIST -> R.string.by_artist
                    Params.TracksOrder.ALBUM -> R.string.by_album
                    Params.TracksOrder.DATE -> R.string.by_date
                    Params.TracksOrder.POS_IN_ALBUM -> R.string.pos_in_album
                }
            )
        } ${
            when {
                params.tracksOrder.second -> "ᐯ"
                else -> "ᐱ"
            }
        }"

    protected abstract fun setTrackOrderLabel(trackOrderLabel: String)

    private fun updateTrackOrderLabel() = setTrackOrderLabel(trackOrderLabel)

    private fun Menu.initTrackOrderMenuItems(order: Params.TracksOrder, isAscending: Boolean) {
        findItem(R.id.order_title).isChecked = order == Params.TracksOrder.TITLE
        findItem(R.id.order_artist).isChecked = order == Params.TracksOrder.ARTIST
        findItem(R.id.order_album).isChecked = order == Params.TracksOrder.ALBUM
        findItem(R.id.order_date).isChecked = order == Params.TracksOrder.DATE
        findItem(R.id.order_pos_in_album).isChecked = order == Params.TracksOrder.POS_IN_ALBUM
        findItem(R.id.asc).isChecked = isAscending
        findItem(R.id.desc).isChecked = !isAscending
    }

    private fun MenuItem.onTrackOrderMenuItemClicked(
        order: Params.TracksOrder,
        isAscending: Boolean
    ) {
        when (itemId) {
            R.id.asc -> params.tracksOrder = order to true
            R.id.desc -> params.tracksOrder = order to false
            R.id.order_title -> params.tracksOrder = Params.TracksOrder.TITLE to isAscending
            R.id.order_artist -> params.tracksOrder = Params.TracksOrder.ARTIST to isAscending
            R.id.order_album -> params.tracksOrder = Params.TracksOrder.ALBUM to isAscending
            R.id.order_date -> params.tracksOrder = Params.TracksOrder.DATE to isAscending
            R.id.order_pos_in_album -> params.tracksOrder =
                Params.TracksOrder.POS_IN_ALBUM to isAscending

            else -> throw IllegalStateException("Unknown menu item was clicked: $itemId")
        }
    }

    private inline val sortedItemList: List<T>
        get() {
            val (order, isAscending) = params.tracksOrder

            return when {
                isAscending -> when (order) {
                    Params.TracksOrder.TITLE -> itemList.sortedBy(Track::title)
                    Params.TracksOrder.ARTIST -> itemList.sortedBy(Track::artist)
                    Params.TracksOrder.ALBUM -> itemList.sortedBy(Track::album)
                    Params.TracksOrder.DATE -> itemList.sortedBy(Track::addDate)
                    Params.TracksOrder.POS_IN_ALBUM -> itemList.sortedBy(Track::trackNumberInAlbum)
                }

                else -> when (order) {
                    Params.TracksOrder.TITLE -> itemList.sortedByDescending(Track::title)
                    Params.TracksOrder.ARTIST -> itemList.sortedByDescending(Track::artist)
                    Params.TracksOrder.ALBUM -> itemList.sortedByDescending(Track::album)
                    Params.TracksOrder.DATE -> itemList.sortedByDescending(Track::addDate)
                    Params.TracksOrder.POS_IN_ALBUM -> itemList.sortedByDescending(Track::trackNumberInAlbum)
                }
            }
        }

    private fun PopupMenu.onTrackOrderMenuItemClicked(
        order: Params.TracksOrder,
        isAscending: Boolean
    ) = setOnMenuItemClickListener { menuItem ->
        runOnIOThread {
            StorageUtil
                .getInstanceAsyncSynchronized()
                .storeTrackOrderLocking(params.tracksOrder)
        }

        menuItem.onTrackOrderMenuItemClicked(order, isAscending)
        updateTrackOrderLabel()
        runOnUIThread { updateUIAsync(sortedItemList) }
        true
    }

    private fun TrackOrderMenu(anchor: View) = PopupMenu(requireContext(), anchor).apply {
        val (order, isAscending) = params.tracksOrder
        menuInflater.inflate(R.menu.menu_track_order, menu)
        menu.initTrackOrderMenuItems(order, isAscending)
        onTrackOrderMenuItemClicked(order, isAscending)
    }

    protected fun showTrackOrderMenu(anchor: View) = TrackOrderMenu(anchor).show()
}