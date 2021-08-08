package com.dinaraparanid.prima.fragments

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import kotlinx.coroutines.*

/**
 * [AbstractTrackListFragment] for tracks of some album
 */

class AlbumTrackListFragment : AbstractTrackListFragment() {
    override lateinit var updater: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlist_track_list, container, false)

        updater = view
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

        layout.findViewById<ImageView>(R.id.shuffle_playlist_track_button).apply {
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

                layout
                    .findViewById<carbon.widget.ConstraintLayout>(R.id.playlist_tracks_image_layout)
                    .apply {
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

                        val playlistImage =
                            findViewById<ImageView>(R.id.playlist_tracks_image).apply {
                                if (!Params.instance.isRoundingPlaylistImage) setCornerRadius(0F)
                            }

                        Glide.with(this@AlbumTrackListFragment)
                            .load(bitmap)
                            .into(playlistImage)
                    }

                trackAmountImage = layout
                    .findViewById<carbon.widget.TextView>(R.id.amount_of_tracks_playlist)
                    .apply {
                        val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                        text = txt
                        typeface = (requireActivity().application as MainApplication)
                            .getFontFromName(Params.instance.font)
                    }

                recyclerView = layout
                    .findViewById<carbon.widget.RecyclerView>(R.id.playlist_track_recycler_view)
                    .apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@AlbumTrackListFragment.adapter?.apply {
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
            .findViewById<carbon.widget.TextView>(R.id.amount_of_tracks_playlist)
            .apply {
                val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                text = txt
                isSelected = true
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        trackOrderTitle = layout
            .findViewById<carbon.widget.TextView>(R.id.playlist_track_order_title)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        updateOrderTitle()

        trackOrderButton = layout
            .findViewById<ImageView>(R.id.playlist_track_order_button)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)

        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this)

        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            itemList.run {
                clear()
                addAll(
                    try {
                        val selection = "${MediaStore.Audio.Media.ALBUM} = ?"

                        val order = "${
                            when (Params.instance.tracksOrder.first) {
                                Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                                Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                                Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                                Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                            }
                        } ${if (Params.instance.tracksOrder.second) "ASC" else "DESC"}"

                        val trackList = mutableListOf<Track>()

                        val projection = mutableListOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DATE_ADDED
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

                        requireActivity().contentResolver.query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projection.toTypedArray(),
                            selection,
                            arrayOf(mainLabelCurText),
                            order
                        ).use { cursor ->
                            if (cursor != null)
                                (requireActivity().application as MainApplication)
                                    .addTracksFromStorage(cursor, trackList)
                        }

                        trackList.distinctBy(Track::path).toPlaylist()
                    } catch (e: Exception) {
                        // Permission to storage not given
                        DefaultPlaylist()
                    }
                )
                Unit
            }
        }
    }
}