package com.dinaraparanid.prima.fragments

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.TrackListViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.concurrent.thread

class TrackListFragment :
    Fragment(),
    SearchView.OnQueryTextListener,
    ContentUpdatable<Playlist>,
    FilterFragment<Track>,
    RecyclerViewUp,
    Loader {
    interface Callbacks {
        fun onTrackSelected(track: Track, tracks: Playlist, ind: Int, needToPlay: Boolean = true)
    }

    private lateinit var trackRecyclerView: RecyclerView
    private lateinit var mainLabelOldText: String
    private lateinit var mainLabelCurText: String
    private lateinit var titleDefault: String

    internal var adapter: TrackAdapter? = null
    internal var genFunc: (() -> Playlist)? = null
    private var callbacks: Callbacks? = null
    internal val playlist = Playlist()
    private val playlistSearch = Playlist()

    private val trackListViewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    companion object {
        private const val MAIN_LABEL_OLD_TEXT_KEY = "main_label_old_text"
        private const val MAIN_LABEL_CUR_TEXT_KEY = "main_label_cur_text"
        private const val START_KEY = "start"
        private const val HIGHLIGHTED_START_KEY = "highlighted_start"
        private const val MAIN_LABEL_KEY = "main_label"
        private const val NO_HIGHLIGHT = "______ЫЫЫЫЫЫЫЫ______"

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            _firstToHighlight: String? = null
        ): TrackListFragment = TrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putString(START_KEY, _firstToHighlight ?: NO_HIGHLIGHT)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        genFunc?.let { playlist.addAll(it()) } ?: load()
        playlistSearch.addAll(playlist.toList())
        adapter = TrackAdapter(playlist)

        trackListViewModel.run {
            load(savedInstanceState?.getBoolean(HIGHLIGHTED_START_KEY))

            mainLabelOldText =
                requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
            mainLabelCurText =
                requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault

            // Thread.sleep(50) // waiting for loading tracks

            if (!highlightedStartLiveData.value!!)
                requireArguments().getString(START_KEY)
                    ?.takeIf { it != NO_HIGHLIGHT }
                    ?.let {
                        (requireActivity().application as MainApplication).run {
                            highlightedRows.add(it)
                            highlightedRows = highlightedRows.distinct().toMutableList()
                        }

                        highlightedStartLiveData.value = true
                    }
        }
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
                setOnRefreshListener {
                    thread {
                        (this@TrackListFragment
                            .requireActivity()
                            .application as MainApplication).load()
                    }
                    playlist.clear()
                    genFunc?.let { playlist.addAll(it()) } ?: load()
                    updateContent(playlist)
                    isRefreshing = false
                }
            }

        trackRecyclerView = updater
            .findViewById<ConstraintLayout>(R.id.track_constraint_layout)
            .findViewById<RecyclerView>(R.id.track_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TrackListFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateUI(playlist)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onStop() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelOldText
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            HIGHLIGHTED_START_KEY,
            trackListViewModel.highlightedStartLiveData.value!!
        )

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            playlistSearch,
            query ?: ""
        )

        playlist.clear()
        playlist.addAll(filteredModelList)
        adapter?.notifyDataSetChanged()

        trackRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun updateUI(src: Playlist) {
        adapter = TrackAdapter(src)
        trackRecyclerView.adapter = adapter
    }

    override fun filter(models: Collection<Track>?, query: String): List<Track> =
        query.lowercase().let { lowerCase ->
            models?.filter {
                lowerCase in it.title.lowercase() || lowerCase in it.artist.lowercase()
            } ?: listOf()
        }

    override fun up() {
        trackRecyclerView.layoutParams =
            (trackRecyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomMargin = 200
            }
    }

    override fun load() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val order = MediaStore.Audio.Media.TITLE + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        requireActivity().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            order
        ).use { cursor ->
            playlist.clear()

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    playlist.add(
                        Track(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getLong(4),
                            cursor.getLong(5)
                        )
                    )
                }
            }
        }
    }

    internal inner class TrackAdapter(private val tracks: Playlist) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        internal inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: Track
            private var ind: Int = 0

            val titleTextView: TextView = itemView.findViewById(R.id.track_title)
            val settingsButton: ImageButton = itemView.findViewById(R.id.track_item_settings)
            val artistsAlbumTextView: TextView =
                itemView.findViewById(R.id.track_author_album)
            private val trackNumberTextView: TextView = itemView.findViewById(R.id.track_number)

            init {
                itemView.setOnClickListener(this)
                titleTextView.setTextColor(ViewSetter.textColor)
                artistsAlbumTextView.setTextColor(ViewSetter.textColor)
                settingsButton.setImageResource(ViewSetter.settingsButtonImage)
            }

            override fun onClick(v: View?) {
                callbacks?.onTrackSelected(track, tracks, ind)
            }

            fun bind(_track: Track, _ind: Int) {
                track = _track
                ind = _ind

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${
                        track.album
                        /*.let {
                                if (it == "<unknown>" || it == track
                                        .path
                                        .split('/')
                                        .takeLast(2)
                                        .first()
                                ) "Unknown album" else it
                            }*/
                    }"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder {
            return TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))
        }

        override fun getItemCount() = tracks.realSize

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            holder.bind(tracks[position], position)

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

            Thread.sleep(10)

            when (tracks[position].path) {
                in highlightedRows -> {
                    val color = Params.getInstance().theme.rgb
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

        fun highlight(track: Track) =
            (requireActivity().application as MainApplication).run {
                highlightedRows.clear()
                highlightedRows.add(track.path)
                highlightedRows = highlightedRows.distinct().toMutableList()
                notifyDataSetChanged()
            }
    }
}