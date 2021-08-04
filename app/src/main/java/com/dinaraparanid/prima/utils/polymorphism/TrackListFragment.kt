package com.dinaraparanid.prima.utils.polymorphism

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.viewmodels.TrackListViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*

/**
 * Ancestor for all tracks fragments
 */

abstract class TrackListFragment :
    TrackListSearchFragment<Track, TrackListFragment.TrackAdapter.TrackHolder>() {
    interface Callbacks : ListFragment.Callbacks {
        /**
         * Plays track or just shows playing bar
         * @param track track to show in playing bar
         * @param tracks tracks from which current playlist' ll be constructed
         * @param needToPlay if true track' ll be played
         * else it' ll be just shown in playing bar
         */

        fun onTrackSelected(
            track: Track,
            tracks: Collection<Track>,
            needToPlay: Boolean = true
        )
    }

    public override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? = null

    override val viewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    private lateinit var trackAmountImage: TextView
    private lateinit var trackOrderButton: ImageButton
    private lateinit var trackOrderTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

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
            .findViewById<ConstraintLayout>(R.id.track_constraint_layout)

        layout.findViewById<ImageButton>(R.id.shuffle_track_button).apply {
            setOnClickListener { updateUI(itemList.shuffled()) }
            Glide.with(this@TrackListFragment)
                .load(ViewSetter.shuffleImage)
                .into(this)
        }

        try {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                loadAsync().await()
                itemListSearch.addAll(itemList)
                adapter = TrackAdapter(itemList).apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                trackAmountImage = layout.findViewById<TextView>(R.id.amount_of_tracks).apply {
                    val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                    text = txt
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

                recyclerView = layout
                    .findViewById<RecyclerView>(R.id.track_recycler_view).apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@TrackListFragment.adapter?.apply {
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

        trackAmountImage = layout.findViewById<TextView>(R.id.amount_of_tracks).apply {
            val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
            text = txt
            isSelected = true
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        trackOrderTitle = layout.findViewById<TextView>(R.id.track_order_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        updateOrderTitle()

        trackOrderButton = layout.findViewById<ImageButton>(R.id.track_order_button).apply {
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

    override fun onResume() {
        super.onResume()

        val act = requireActivity() as MainActivity

        if (act.needToUpdate) {
            updateUIOnChangeTracks()
            act.needToUpdate = false
        }
    }

    override fun updateUI(src: List<Track>) {
        try {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                adapter = TrackAdapter(src).apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }
                recyclerView.adapter = adapter

                val text = "${resources.getString(R.string.tracks)}: ${src.size}"
                trackAmountImage.text = text
            }
        } catch (ignored: Exception) {
        }
    }

    override fun filter(models: Collection<Track>?, query: String): List<Track> =
        query.lowercase().let { lowerCase ->
            models?.filter {
                val t =
                    if (SearchOrder.TITLE in searchOrder) lowerCase in it.title.lowercase() else false

                val ar =
                    if (SearchOrder.ARTIST in searchOrder) lowerCase in it.artist.lowercase() else false

                val al =
                    if (SearchOrder.ALBUM in searchOrder) lowerCase in it.playlist.lowercase() else false

                t || ar || al
            } ?: listOf()
        }

    override fun onQueryTextChange(query: String?): Boolean {
        super.onQueryTextChange(query)
        val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
        trackAmountImage.text = txt
        return true
    }

    internal fun updateUIOnChangeTracks() {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().await()
            updateUI()
        }
    }

    /**
     * [RecyclerView.Adapter] for [TrackListFragment]
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: Track

            val titleTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_title)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            val settingsButton: ImageButton = itemView.findViewById(R.id.track_item_settings)

            val artistsAlbumTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_author_album)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val trackNumberTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_number)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            init {
                itemView.setOnClickListener(this)
                Glide.with(this@TrackListFragment)
                    .load(ViewSetter.settingsButtonImage)
                    .into(settingsButton)
            }

            override fun onClick(v: View?) {
                (callbacks as Callbacks?)?.onTrackSelected(track, tracks)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: Track) {
                track = _track

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${
                        NativeLibrary.playlistTitle(
                            track.playlist.toByteArray(),
                            track.path.toByteArray(),
                            resources.getString(R.string.unknown_album).toByteArray()
                        )
                    }"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            holder.bind(tracks[position])

            val trackTitle = holder.titleTextView
            val trackAlbumArtist = holder.artistsAlbumTextView
            val settingsButton = holder.settingsButton
            val highlightedRows = (requireActivity().application as MainApplication).highlightedRows

            settingsButton.setOnClickListener {
                (requireActivity() as MainActivity)
                    .trackSettingsButtonAction(
                        it,
                        tracks[position],
                        BottomSheetBehavior.STATE_COLLAPSED
                    )
            }

            when (tracks[position].path) {
                in highlightedRows -> {
                    val color = Params.instance.theme.rgb
                    trackTitle.setTextColor(color)
                    trackAlbumArtist.setTextColor(color)
                }

                else -> {
                    val color = ViewSetter.textColor
                    trackTitle.setTextColor(color)
                    trackAlbumArtist.setTextColor(color)
                }
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param track track to highlight
         */

        @SuppressLint("NotifyDataSetChanged")
        fun highlight(track: Track): Unit =
            (requireActivity().application as MainApplication).run {
                highlightedRows.clear()
                highlightedRows.add(track.path)
                highlightedRows = highlightedRows.distinct().toMutableList()
                notifyDataSetChanged()
            }
    }

    /**
     * Updates title of tracks ordering
     */

    private fun updateOrderTitle() = trackOrderTitle.run {
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
}