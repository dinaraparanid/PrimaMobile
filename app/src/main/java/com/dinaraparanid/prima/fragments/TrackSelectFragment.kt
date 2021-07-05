package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.CheckBox
import android.widget.SearchView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.ListFragment
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.utils.polymorphism.updateContent
import com.dinaraparanid.prima.viewmodels.TrackSelectedViewModel

class TrackSelectFragment : ListFragment<Track, TrackSelectFragment.TrackAdapter.TrackHolder>() {
    private lateinit var playlistTracks: List<Track>
    private val addSet = mutableSetOf<Track>()
    private val removeSet = mutableSetOf<Track>()

    override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? =
        TrackAdapter(mutableListOf())

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[TrackSelectedViewModel::class.java]
    }

    companion object {
        private const val PLAYLIST_TRACKS_KEY = "playlist_tracks"

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            playlistTracks: Playlist
        ): TrackSelectFragment = TrackSelectFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putSerializable(PLAYLIST_TRACKS_KEY, playlistTracks)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault

        load()
        itemListSearch.addAll(itemList)
        adapter = TrackAdapter(itemList)

        playlistTracks = (requireArguments().getSerializable(PLAYLIST_TRACKS_KEY) as Playlist)
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
                setColorSchemeColors(Params.getInstance().theme.rgb)
                setOnRefreshListener {
                    itemList.clear()
                    load()
                    updateContent(itemList)
                    isRefreshing = false
                }
            }

        recyclerView = updater
            .findViewById<ConstraintLayout>(R.id.track_constraint_layout)
            .findViewById<RecyclerView>(R.id.track_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TrackSelectFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_select_track, menu)
        (menu.findItem(R.id.select_track_find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.accept_selected_tracks) {
            requireActivity().supportFragmentManager.popBackStack()

            addSet
                .map { it.asCustom(mainLabelOldText) }
                .forEach(CustomPlaylistsRepository.instance::addTrack)

            removeSet.map { it.path }.forEach(CustomPlaylistsRepository.instance::removeTrack)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            itemList,
            query ?: ""
        )

        itemListSearch.clear()
        itemListSearch.addAll(filteredModelList)
        adapter?.notifyDataSetChanged()

        recyclerView.scrollToPosition(0)
        return true
    }

    override fun updateUI(src: List<Track>) {
        adapter = TrackAdapter(src)
        recyclerView.adapter = adapter
    }

    override fun filter(models: Collection<Track>?, query: String): List<Track> =
        query.lowercase().let { lowerCase ->
            models?.filter {
                lowerCase in it.title.lowercase()
                        || lowerCase in it.artist.lowercase()
                        || lowerCase in it.playlist.lowercase()
            } ?: listOf()
        }

    override fun load() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val order = MediaStore.Audio.Media.TITLE + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        requireActivity().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            order
        ).use { cursor ->
            itemList.clear()

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    itemList.add(
                        Track(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getLong(4)
                        )
                    )
                }
            }
        }
    }

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        private val click = { track: Track, trackSelector: CheckBox ->
            when {
                trackSelector.isChecked -> addSet.add(track)

                else -> when (track) {
                    in addSet -> addSet.remove(track)
                    else -> removeSet.add(track)
                }
            }
        }

        inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: Track
            private var ind: Int = 0

            private val titleTextView: TextView = itemView.findViewById(R.id.select_track_title)
            internal val trackSelector: CheckBox = itemView.findViewById(R.id.track_selector_button)

            private val artistsAlbumTextView: TextView =
                itemView.findViewById(R.id.select_track_author_album)

            private val trackNumberTextView: TextView =
                itemView.findViewById(R.id.select_track_number)

            init {
                itemView.setOnClickListener(this)
                titleTextView.setTextColor(ViewSetter.textColor)
                artistsAlbumTextView.setTextColor(ViewSetter.textColor)
            }

            override fun onClick(v: View?): Unit = Unit // click(track, trackSelector)

            fun bind(_track: Track, _ind: Int) {
                track = _track
                ind = _ind

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${track.playlist}"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
                trackSelector.isChecked = track in addSet || track in playlistTracks
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_select_track, parent, false))

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            holder.bind(tracks[position], position)

            val trackSelector = holder.trackSelector
            trackSelector.setOnClickListener { click(tracks[position], trackSelector) }
        }
    }
}