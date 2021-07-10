package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.CheckBox
import android.widget.SearchView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.ListFragment
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.utils.polymorphism.updateContent
import com.dinaraparanid.prima.viewmodels.TrackSelectedViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class TrackSelectFragment : ListFragment<Track, TrackSelectFragment.TrackAdapter.TrackHolder>() {
    private val playlistTracks = mutableListOf<Track>()
    private var playlistId = 0L

    override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? =
        TrackAdapter(mutableListOf())

    override val viewModel: TrackSelectedViewModel by lazy {
        ViewModelProvider(this)[TrackSelectedViewModel::class.java]
    }

    companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"
        private const val PLAYLIST_TRACKS_KEY = "playlist_tracks"
        private const val SELECT_ALL_KEY = "select_all"
        private const val ADD_SET_KEY = "add_set"
        private const val REMOVE_SET_KEY = "remove_set"

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            playlistId: Long,
            playlistTracks: Playlist
        ): TrackSelectFragment = TrackSelectFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
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

        runBlocking { loadAsync().await() }
        itemListSearch.addAll(itemList)

        playlistTracks.addAll((requireArguments().getSerializable(PLAYLIST_TRACKS_KEY) as Playlist))
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
        adapter = TrackAdapter(itemList)

        viewModel.load(
            savedInstanceState?.getBoolean(SELECT_ALL_KEY),
            savedInstanceState?.getSerializable(ADD_SET_KEY) as Array<Track>?,
            savedInstanceState?.getSerializable(REMOVE_SET_KEY) as Array<Track>?
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_track_list, container, false)
        titleDefault = resources.getString(R.string.tracks)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.select_track_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.getInstance().theme.rgb)
                setOnRefreshListener {
                    itemList.clear()
                    runBlocking { loadAsync().await() }
                    updateContent(itemList)
                    isRefreshing = false
                }
            }

        recyclerView = updater
            .findViewById<ConstraintLayout>(R.id.select_track_constraint_layout)
            .findViewById<RecyclerView>(R.id.select_track_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TrackSelectFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            SELECT_ALL_KEY,
            viewModel.selectAllLiveData.value!!
        )

        outState.putSerializable(
            ADD_SET_KEY,
            viewModel.addSetLiveData.value!!.toTypedArray()
        )

        outState.putSerializable(
            REMOVE_SET_KEY,
            viewModel.removeSetLiveData.value!!.toTypedArray()
        )

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_select, menu)
        (menu.findItem(R.id.select_find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.accept_selected_items -> {
                requireActivity().supportFragmentManager.popBackStack()
                val task = CustomPlaylistsRepository.instance.getPlaylistAsync(mainLabelOldText)

                viewModel.removeSetLiveData.value!!.map { it.path }.forEach {
                    CustomPlaylistsRepository.instance.removeTrack(it, playlistId)
                }

                val id = runBlocking { task.await()!!.id }

                viewModel.addSetLiveData.value!!
                    .map {
                        CustomPlaylistTrack(
                            0,
                            it.title,
                            it.artist,
                            it.playlist,
                            id,
                            it.path,
                            it.duration
                        )
                    }
                    .forEach(CustomPlaylistsRepository.instance::addTrack)
            }

            R.id.select_all -> {
                when {
                    viewModel.selectAllLiveData.value!! -> {
                        viewModel.removeSetLiveData.value!!.apply {
                            addAll(viewModel.addSetLiveData.value!!)
                            addAll(playlistTracks)
                        }

                        viewModel.addSetLiveData.value!!.clear()
                    }

                    else -> {
                        viewModel.removeSetLiveData.value!!.clear()
                        viewModel.addSetLiveData.value!!.addAll(itemList)
                    }
                }

                viewModel.selectAllLiveData.value = !viewModel.selectAllLiveData.value!!
                updateUI(itemList)
            }
        }

        return super.onOptionsItemSelected(item)
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

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async {
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
    }

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        private val click = { track: Track, trackSelector: CheckBox ->
            when {
                trackSelector.isChecked -> viewModel.addSetLiveData.value!!.add(track)

                else -> when (track) {
                    in viewModel.addSetLiveData.value!! ->
                        viewModel.addSetLiveData.value!!.remove(track)

                    else -> viewModel.removeSetLiveData.value!!.add(track)
                }
            }
        }

        internal val tracksSet: Set<String> by lazy {
            playlistTracks.map { it.path }.toSet()
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

                trackSelector.isChecked = track !in viewModel.removeSetLiveData.value!!
                        && (track in viewModel.addSetLiveData.value!!
                        || track.path in tracksSet)
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