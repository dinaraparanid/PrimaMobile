package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ImageView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import kotlinx.coroutines.*

/**
 * Typical track list fragment ancestor
 * without no special view features
 */

abstract class TypicalTrackListFragment : OnlySearchMenuTrackListFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_list, container, false)
        titleDefault = resources.getString(R.string.tracks)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.track_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.instance.theme.rgb)
                setOnRefreshListener {
                    try {
                        viewModel.viewModelScope.launch(Dispatchers.Main) {
                            loadAsync().await()
                            updateUI(itemList)
                            isRefreshing = false
                        }
                    } catch (ignored: Exception) {
                        // permissions not given
                    }
                }
            }

        val layout = updater
            .findViewById<carbon.widget.ConstraintLayout>(R.id.track_constraint_layout)

        emptyTextView = layout.findViewById<TextView>(R.id.track_list_empty).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        layout.findViewById<ImageView>(R.id.shuffle_track_button).apply {
            setOnClickListener { updateUI(itemList.shuffled()) }
        }

        try {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                loadAsync().await()
                setEmptyTextViewVisibility(itemList)
                itemListSearch.addAll(itemList)
                adapter = TrackAdapter(itemList).apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                trackAmountImage = layout
                    .findViewById<carbon.widget.TextView>(R.id.amount_of_tracks)
                    .apply {
                        val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                        text = txt
                        typeface = (requireActivity().application as MainApplication)
                            .getFontFromName(Params.instance.font)
                    }

                recyclerView = layout
                    .findViewById<carbon.widget.RecyclerView>(R.id.track_recycler_view)
                    .apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@TypicalTrackListFragment.adapter?.apply {
                            stateRestorationPolicy =
                                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                    }

                if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
            }
        } catch (ignored: Exception) {
            // permissions not given
        }

        trackAmountImage = layout
            .findViewById<carbon.widget.TextView>(R.id.amount_of_tracks)
            .apply {
                val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                text = txt
                isSelected = true
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        trackOrderTitle = layout
            .findViewById<carbon.widget.TextView>(R.id.track_order_title)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        updateOrderTitle()

        trackOrderButton = layout
            .findViewById<ImageView>(R.id.track_order_button)
            .apply {
                setOnClickListener {
                    PopupMenu(requireContext(), it).run {
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
                            StorageUtil(requireContext()).storeTrackOrder(Params.instance.tracksOrder)
                            updateUI(Params.sortedTrackList(itemList))
                            true
                        }

                        show()
                    }
                }
            }

        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }
}