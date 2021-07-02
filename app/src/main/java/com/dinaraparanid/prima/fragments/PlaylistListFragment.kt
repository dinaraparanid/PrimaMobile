package com.dinaraparanid.prima.fragments

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.PlaylistListViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistListFragment :
    Fragment(),
    SearchView.OnQueryTextListener,
    ContentUpdatable<List<Playlist>>,
    FilterFragment<Playlist>,
    RecyclerViewUp,
    Loader {
    interface Callbacks {
        fun onPlaylistSelected(title: String, playlistGen: () -> Playlist)
    }

    private lateinit var playlistRecyclerView: RecyclerView
    private lateinit var mainLabelOldText: String
    private lateinit var mainLabelCurText: String
    private lateinit var titleDefault: String

    private var adapter: PlaylistAdapter? = null
    private var callbacks: Callbacks? = null
    private val playlists = mutableListOf<Playlist>()
    private val playlistsSearch = mutableListOf<Playlist>()

    internal val playlistListViewModel: PlaylistListViewModel by lazy {
        ViewModelProvider(this)[PlaylistListViewModel::class.java]
    }

    companion object {
        private const val MAIN_LABEL_OLD_TEXT_KEY = "main_label_old_text"
        private const val MAIN_LABEL_CUR_TEXT_KEY = "main_label_cur_text"

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String
        ): PlaylistListFragment = PlaylistListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
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
        load()
        playlistsSearch.addAll(playlists)
        adapter = PlaylistAdapter(playlistsSearch)

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
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        titleDefault = resources.getString(R.string.playlists)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.playlist_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.getInstance().theme.rgb)
                setOnRefreshListener {
                    load()
                    updateContent(playlists)
                    isRefreshing = false
                }
            }

        playlistRecyclerView = updater
            .findViewById<ConstraintLayout>(R.id.playlist_constraint_layout)
            .findViewById<RecyclerView>(R.id.playlist_recycler_view)
            .apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = this@PlaylistListFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
                addItemDecoration(HorizontalSpaceItemDecoration(30))
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateUI(playlistsSearch.toList())
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

    override fun onResume() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_playlist_add_search, menu)
        (menu.findItem(R.id.playlist_search).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.add_playlist -> false // TODO: add custom playlists
        else -> super.onOptionsItemSelected(item)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            playlists,
            query ?: ""
        )

        playlistsSearch.clear()
        playlistsSearch.addAll(filteredModelList)
        adapter!!.notifyDataSetChanged()
        updateUI(playlistsSearch)

        playlistRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun updateUI(src: List<Playlist>) {
        adapter = PlaylistAdapter(src)
        playlistRecyclerView.adapter = adapter
    }

    override fun filter(
        models: Collection<Playlist>?,
        query: String
    ): List<Playlist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override fun up() {
        playlistRecyclerView.layoutParams =
            (playlistRecyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomMargin = 200
            }
    }

    override fun load() {
        requireActivity().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Albums.ALBUM),
            null,
            null,
            MediaStore.Audio.Media.ALBUM + " ASC"
        ).use { cursor ->
            playlists.clear()

            if (cursor != null) {
                val playlistList = mutableListOf<Playlist>()

                while (cursor.moveToNext()) {
                    val albumTitle = cursor.getString(0)

                    (requireActivity().application as MainApplication).allTracks
                        .firstOrNull { it.playlist == albumTitle }
                        ?.let { track ->
                            playlistList.add(
                                DefaultPlaylist(
                                    albumTitle,
                                    tracks = mutableListOf(track) // album image
                                )
                            )
                        }
                }

                playlists.addAll(playlistList.distinctBy { it.title })
            }
        }
    }

    internal fun loadTracks(playlist: Playlist): Playlist {
        val selection = "${MediaStore.Audio.Media.ALBUM} = ?"
        val order = MediaStore.Audio.Media.TITLE + " ASC"
        val trackList = mutableListOf<Track>()

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
            arrayOf(playlist.title),
            order
        ).use { cursor ->
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    trackList.add(
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

        return trackList.distinctBy { it.path }.toPlaylist()
    }

    internal inner class PlaylistAdapter(private val playlists: List<Playlist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
        internal inner class PlaylistHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var playlist: Playlist

            private val titleTextView = itemView
                .findViewById<TextView>(R.id.playlist_title)
                .apply { setTextColor(ViewSetter.textColor) }

            private val playlistImage: ImageView = itemView
                .findViewById<CardView>(R.id.playlist_card_view)
                .findViewById(R.id.playlist_image)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                callbacks?.onPlaylistSelected(playlist.title) { loadTracks(playlist) }
            }

            fun bind(_playlist: Playlist) {
                playlist = _playlist
                titleTextView.text = playlist.title

                playlistListViewModel.viewModelScope.launch {
                    playlistImage.setImageBitmap(
                        withContext(playlistListViewModel.viewModelScope.coroutineContext) {
                            (requireActivity().application as MainApplication).run {
                                albumImages.getOrPut(playlist.title) {
                                    getAlbumPicture(playlist.currentTrack.path)
                                }
                            }
                        }
                    )
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistHolder(layoutInflater.inflate(R.layout.list_item_playlist, parent, false))

        override fun getItemCount() = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) {
            holder.bind(playlists[position])
        }
    }
}