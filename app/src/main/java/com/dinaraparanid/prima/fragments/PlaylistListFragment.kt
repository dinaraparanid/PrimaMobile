package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.dialogs.NewPlaylistDialog
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.PlaylistListViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PlaylistListFragment :
    ListFragment<Playlist, PlaylistListFragment.PlaylistAdapter.PlaylistHolder>() {
    interface Callbacks : ListFragment.Callbacks {
        fun onPlaylistSelected(
            id: Long,
            title: String,
            custom: Boolean,
            playlistGen: () -> Playlist
        )
    }

    override var adapter: RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[PlaylistListViewModel::class.java]
    }

    companion object {
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
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        titleDefault = resources.getString(R.string.playlists)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.playlist_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.getInstance().theme.rgb)
                setOnRefreshListener {
                    load()
                    updateContent(itemList)
                    isRefreshing = false
                }
            }

        recyclerView = updater
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

    override fun onStop() {
        (requireActivity() as MainActivity).selectButton.isVisible = false
        super.onStop()
    }

    override fun onResume() {
        (requireActivity() as MainActivity).selectButton.isVisible = true

        load()
        itemListSearch.addAll(itemList)
        adapter = PlaylistAdapter(itemListSearch)
        updateContent(itemList)

        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_playlists_menu, menu)
        (menu.findItem(R.id.playlist_search).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.add_playlist -> NewPlaylistDialog(this)
            .show(parentFragmentManager, null)
            .run { false }

        else -> super.onOptionsItemSelected(item)
    }

    override fun updateUI(src: List<Playlist>) {
        adapter = PlaylistAdapter(src)
        recyclerView.adapter = adapter
    }

    override fun filter(
        models: Collection<Playlist>?,
        query: String
    ): List<Playlist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override fun load(): Unit = when (mainLabelCurText) {
        resources.getString(R.string.playlists) -> itemList.run {
            val task = CustomPlaylistsRepository.instance.playlistsAsync

            clear()
            addAll(runBlocking { task.await() }.map(::CustomPlaylist))
            Unit
        }

        else -> requireActivity().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Albums.ALBUM),
            null,
            null,
            MediaStore.Audio.Media.ALBUM + " ASC"
        ).use { cursor ->
            itemList.clear()

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

                itemList.addAll(playlistList.distinctBy { it.title })
            }
        }
    }

    internal fun loadTracks(playlist: Playlist) = when (playlist) {
        is CustomPlaylist -> CustomPlaylist(
            playlist.title,
            runBlocking {
                CustomPlaylistsRepository.instance
                    .getTracksOfPlaylistAsync(playlist.title)
                    .await()
            }
        )

        else -> {
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

            trackList.distinctBy { it.path }.toPlaylist()
        }
    }

    inner class PlaylistAdapter(private val playlists: List<Playlist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
        inner class PlaylistHolder(view: View) :
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

            override fun onClick(v: View?): Unit = (callbacks as Callbacks?)?.onPlaylistSelected(
                when (mainLabelCurText) {
                    resources.getString(R.string.playlists) -> runBlocking {
                        CustomPlaylistsRepository.instance
                            .getPlaylistAsync(playlist.title)
                            .await()!!
                            .id
                    }

                    else -> 0
                },
                playlist.title,
                mainLabelCurText == resources.getString(R.string.playlists)
            ) { loadTracks(playlist) } ?: Unit

            fun bind(_playlist: Playlist) {
                playlist = _playlist
                titleTextView.text = playlist.title

                viewModel.viewModelScope.launch {
                    playlist.takeIf { it.size > 0 }?.run {
                        suspend {
                            val app = (requireActivity().application as MainApplication)
                            playlistImage.setImageBitmap(
                                app.albumImages.getOrPut(playlist.title) {
                                    app.getAlbumPicture(currentTrack.path)
                                }
                            )
                        }.invoke()
                    }
                }

                /*viewModel.viewModelScope.launch {
                    playlistImage.setImageBitmap(
                        withContext(viewModel.viewModelScope.coroutineContext) {
                            (requireActivity().application as MainApplication).run {
                                albumImages.getOrPut(playlist.title) {
                                    playlist.takeIf { it.size > 0 }
                                        ?.run { getAlbumPicture(currentTrack.path) }
                                        ?: run {
                                            val albumPicture = BitmapFactory
                                                .decodeResource(resources, R.drawable.album_default)
                                            val width = albumPicture.width
                                            val height = albumPicture.height

                                            Bitmap.createBitmap(
                                                albumPicture,
                                                0,
                                                0,
                                                width,
                                                height,
                                                Matrix(),
                                                false
                                            )
                                        }
                                }
                            }
                        }
                    )
                }*/
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(layoutInflater.inflate(R.layout.list_item_playlist, parent, false))

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(playlists[position])
    }
}