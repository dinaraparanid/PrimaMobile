package com.dinaraparanid.prima.utils.polymorphism

import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import carbon.widget.TextView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil

/**
 * Track [ListFragment] with search functions
 */

abstract class TrackListSearchFragment<T, A, VH, B> :
    MainActivityUpdatingListFragment<T, A, VH, B>()
        where T : AbstractTrack,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH>,
              B : ViewDataBinding {
    /** Search  */
    internal enum class SearchOrder {
        TITLE, ARTIST, ALBUM
    }

    protected abstract var amountOfTracks: TextView?
    protected abstract var trackOrderTitle: TextView?

    private val searchOrder: MutableList<SearchOrder> by lazy {
        StorageUtil(requireContext())
            .loadTrackSearchOrder()
            ?.toMutableList()
            ?: mutableListOf(
                SearchOrder.TITLE,
                SearchOrder.ARTIST,
                SearchOrder.ALBUM
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        amountOfTracks = null
        trackOrderTitle = null
    }

    final override fun filter(models: Collection<T>?, query: String) = query.lowercase().let { lowerCase ->
        models?.filter {
            val t = when (SearchOrder.TITLE) {
                in searchOrder -> lowerCase in it.title.lowercase()
                else -> false
            }

            val ar = when (SearchOrder.ARTIST) {
                in searchOrder -> lowerCase in it.artist.lowercase()
                else -> false
            }

            val al = when (SearchOrder.ALBUM) {
                in searchOrder -> lowerCase in it.playlist.lowercase()
                else -> false
            }

            t || ar || al
        } ?: listOf()
    }

    /**
     * Shows menu with search params to select
     */

    protected fun selectSearch() = PopupMenu(
        requireContext(),
        fragmentActivity.switchToolbar
    ).run {
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

    /**
     * Updates title of tracks ordering
     */

    internal fun updateOrderTitle(): Unit = trackOrderTitle!!.run {
        val txt = "${
            resources.getString(
                when (Params.instance.tracksOrder.first) {
                    Params.Companion.TracksOrder.TITLE -> R.string.by_title
                    Params.Companion.TracksOrder.ARTIST -> R.string.by_artist
                    Params.Companion.TracksOrder.ALBUM -> R.string.by_album
                    else -> R.string.by_date
                }
            )
        } ${
            when {
                Params.instance.tracksOrder.second -> "ᐯ"
                else -> "ᐱ"
            }
        }"

        text = txt
    }

    internal fun onTrackOrderButtonPressed(view: View) = PopupMenu(requireContext(), view).run {
        menuInflater.inflate(R.menu.menu_track_order, menu)

        val f = Params.instance.tracksOrder.first
        val s = Params.instance.tracksOrder.second

        menu.findItem(R.id.asc).isChecked = Params.instance.tracksOrder.second
        menu.findItem(R.id.desc).isChecked = !Params.instance.tracksOrder.second

        menu.findItem(R.id.order_title).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.TITLE

        menu.findItem(R.id.order_artist).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.ARTIST

        menu.findItem(R.id.order_album).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.ALBUM

        menu.findItem(R.id.order_date).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.DATE

        setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.asc -> Params.instance.tracksOrder = f to true
                R.id.desc -> Params.instance.tracksOrder = f to false

                R.id.order_title -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.TITLE to s

                R.id.order_artist -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.ARTIST to s

                R.id.order_album -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.ALBUM to s

                else -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.DATE to s
            }

            updateOrderTitle()
            StorageUtil(requireContext().applicationContext).storeTrackOrder(Params.instance.tracksOrder)
            runOnUIThread { updateUIAsync(Params.sortedTrackList(itemList)) }
            true
        }

        show()
    }

    internal fun onShuffleButtonPressed() = runOnUIThread { updateUIAsync(itemList.shuffled()) }
}