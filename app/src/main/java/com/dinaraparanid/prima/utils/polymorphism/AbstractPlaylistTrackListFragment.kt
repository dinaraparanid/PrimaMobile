package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ancestor for all playlist track list fragments
 */

abstract class AbstractPlaylistTrackListFragment : OnlySearchMenuTrackListFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlist_track_list, container, false)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.playlist_track_swipe_refresh_layout)
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
            .findViewById<ConstraintLayout>(R.id.playlist_track_constraint_layout)

        emptyTextView = layout.findViewById<TextView>(R.id.playlist_track_list_empty).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        layout.findViewById<ImageButton>(R.id.shuffle_playlist_track_button).apply {
            setOnClickListener { updateUI(itemList.shuffled()) }
            Glide.with(this@AbstractPlaylistTrackListFragment)
                .load(ViewSetter.shuffleImage)
                .into(this)
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

                layout
                    .findViewById<ConstraintLayout>(R.id.playlist_tracks_image_layout)
                    .apply {
                        findViewById<CardView>(R.id.playlist_image_card_view).run {
                            if (!Params.instance.isRoundingPlaylistImage) radius = 0F
                        }

                        val bitmap = (requireActivity().application as MainApplication)
                            .run {
                                when {
                                    itemList.isEmpty() -> getAlbumPictureAsync(
                                        "",
                                        true
                                    )
                                    else -> getAlbumPictureAsync(
                                        itemList.first().path,
                                        Params.instance.showPlaylistsImages
                                    )
                                }
                            }
                            .await()

                        setBackgroundColor(
                            Palette.from(bitmap)
                                .generate {}
                                .get()
                                .run {
                                    val default = getDominantColor(Params.instance.theme.rgb)
                                    if (Params.instance.theme.isNight) getLightMutedColor(default)
                                    else getDarkVibrantColor(default)
                                }
                        )

                        Glide.with(this@AbstractPlaylistTrackListFragment)
                            .load(bitmap)
                            .into(findViewById(R.id.playlist_tracks_image))
                    }

                trackAmountImage = layout
                    .findViewById<TextView>(R.id.amount_of_tracks_playlist)
                    .apply {
                        val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                        text = txt
                        typeface = (requireActivity().application as MainApplication)
                            .getFontFromName(Params.instance.font)
                    }

                recyclerView = layout
                    .findViewById<RecyclerView>(R.id.playlist_track_recycler_view).apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@AbstractPlaylistTrackListFragment.adapter?.apply {
                            stateRestorationPolicy =
                                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        addItemDecoration(DividerItemDecoration(requireActivity()))
                    }

                if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
            }
        } catch (ignored: Exception) {
            // permissions not given
        }

        trackAmountImage = layout.findViewById<TextView>(R.id.amount_of_tracks_playlist).apply {
            val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
            text = txt
            isSelected = true
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        trackOrderTitle = layout.findViewById<TextView>(R.id.playlist_track_order_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        updateOrderTitle()

        trackOrderButton = layout
            .findViewById<ImageButton>(R.id.playlist_track_order_button)
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