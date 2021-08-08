package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.FloatingActionButton
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.RenamePlaylistDialog
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*

/**
 * [AbstractTrackListFragment] for user's playlists
 */

class CustomPlaylistTrackListFragment : AbstractTrackListFragment() {
    private var playlistId = 0L
    val mainLabel: String by lazy { mainLabelCurText }

    internal companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param playlistId id of playlist
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            playlistId: Long,
        ) = CustomPlaylistTrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_custom_playlist_track_list, container, false)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.custom_playlist_track_swipe_refresh_layout)
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
            .findViewById<ConstraintLayout>(R.id.custom_playlist_track_constraint_layout)

        emptyTextView = layout.findViewById<TextView>(R.id.custom_playlist_track_list_empty).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        layout.findViewById<ImageView>(R.id.shuffle_custom_playlist_track_button).apply {
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
                    .findViewById<carbon.widget.ConstraintLayout>(R.id.custom_playlist_tracks_image_layout)
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
                            findViewById<ImageView>(R.id.custom_playlist_tracks_image).apply {
                                if (!Params.instance.isRoundingPlaylistImage) setCornerRadius(0F)
                            }

                        Glide.with(this@CustomPlaylistTrackListFragment)
                            .load(bitmap)
                            .into(playlistImage)
                    }

                trackAmountImage = layout
                    .findViewById<carbon.widget.TextView>(R.id.amount_of_tracks_custom_playlist)
                    .apply {
                        val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                        text = txt
                        typeface = (requireActivity().application as MainApplication)
                            .getFontFromName(Params.instance.font)
                    }

                recyclerView = layout
                    .findViewById<carbon.widget.RecyclerView>(R.id.custom_playlist_track_recycler_view)
                    .apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@CustomPlaylistTrackListFragment.adapter?.apply {
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
            .findViewById<carbon.widget.TextView>(R.id.amount_of_tracks_custom_playlist)
            .apply {
                val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                text = txt
                isSelected = true
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        trackOrderTitle = layout
            .findViewById<carbon.widget.TextView>(R.id.custom_playlist_track_order_title)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        updateOrderTitle()

        trackOrderButton = layout
            .findViewById<ImageView>(R.id.custom_playlist_track_order_button)
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

        view.findViewById<FloatingActionButton>(R.id.add_track_button).apply {
            setOnClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in,
                        R.anim.slide_out,
                        R.anim.slide_in,
                        R.anim.slide_out
                    )
                    .replace(
                        R.id.fragment_container,
                        TrackSelectFragment.newInstance(
                            mainLabelCurText,
                            resources.getString(R.string.tracks),
                            playlistId,
                            itemList.toPlaylist()
                        )
                    )
                    .addToBackStack(null)
                    .apply {
                        (requireActivity() as MainActivity).sheetBehavior.state =
                            BottomSheetBehavior.STATE_COLLAPSED
                    }
                    .commit()
            }
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_custom_playlist_menu, menu)

        (menu.findItem(R.id.custom_playlist_search).actionView as SearchView).run {
            setOnQueryTextListener(this@CustomPlaylistTrackListFragment)
        }

        menu.findItem(R.id.cp_find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rename_playlist -> RenamePlaylistDialog(this)
                .show(requireActivity().supportFragmentManager, null)

            R.id.remove_playlist -> AreYouSureDialog(
                R.string.ays_remove_playlist,
            ) {
                CustomPlaylistsRepository.instance.run {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        removePlaylistAsync(mainLabelCurText)
                        removeTracksOfPlaylistAsync(mainLabelCurText)
                    }
                }
                requireActivity().supportFragmentManager.popBackStack()
            }.show(requireActivity().supportFragmentManager, null)
        }

        return super.onOptionsItemSelected(item)
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            val task = CustomPlaylistsRepository.instance
                .getTracksOfPlaylistAsync(mainLabelCurText)

            itemList.clear()
            itemList.addAll(Params.sortedTrackList(task.await()))
            Unit
        }
    }

    /**
     * Renames main label when playlist is rename
     * @param title new playlist's title
     */

    fun renameTitle(title: String) {
        mainLabelCurText = title
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
    }
}