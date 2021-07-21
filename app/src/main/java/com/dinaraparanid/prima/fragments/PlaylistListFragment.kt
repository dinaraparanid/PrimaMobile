package com.dinaraparanid.prima.fragments

import android.content.res.Configuration
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
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.NewPlaylistDialog
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.PlaylistListViewModel
import kotlinx.coroutines.*

class PlaylistListFragment :
    ListFragment<Playlist, PlaylistListFragment.PlaylistAdapter.PlaylistHolder>() {
    interface Callbacks : ListFragment.Callbacks {
        fun onPlaylistSelected(
            id: Long,
            title: String,
            custom: Boolean,
        )
    }

    override var adapter: RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[PlaylistListViewModel::class.java]
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
                setColorSchemeColors(Params.instance.theme.rgb)
                setOnRefreshListener {
                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        loadAsync().await()
                        updateContent(itemList)
                        isRefreshing = false
                    }
                }
            }

        recyclerView = updater
            .findViewById<ConstraintLayout>(R.id.playlist_constraint_layout)
            .findViewById<RecyclerView>(R.id.playlist_recycler_view)
            .apply {
                layoutManager = when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_PORTRAIT ->
                        when (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) {
                            Configuration.SCREENLAYOUT_SIZE_NORMAL -> GridLayoutManager(context, 2)
                            Configuration.SCREENLAYOUT_SIZE_LARGE -> GridLayoutManager(context, 3)
                            else -> GridLayoutManager(context, 2)
                        }

                    else -> when (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) {
                        Configuration.SCREENLAYOUT_SIZE_NORMAL -> GridLayoutManager(context, 3)
                        Configuration.SCREENLAYOUT_SIZE_LARGE -> GridLayoutManager(context, 4)
                        else -> GridLayoutManager(context, 3)
                    }
                }

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

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().await()
            itemListSearch.addAll(itemList)
            adapter = PlaylistAdapter(itemListSearch)
            updateUI(itemList)
        }

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
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = PlaylistAdapter(src)
            recyclerView.adapter = adapter
        }
    }

    override fun filter(
        models: Collection<Playlist>?,
        query: String
    ): List<Playlist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            if ((requireActivity().application as MainApplication).checkAndRequestPermissions()) {
                when (mainLabelCurText) {
                    resources.getString(R.string.playlists) -> itemList.run {
                        val task = CustomPlaylistsRepository.instance.playlistsAsync

                        clear()
                        addAll(task.await().map(::CustomPlaylist))
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
            }
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

            override fun onClick(v: View?): Unit = (callbacks as Callbacks).onPlaylistSelected(
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
            )

            fun bind(_playlist: Playlist) {
                playlist = _playlist
                titleTextView.text = playlist.title

                viewModel.viewModelScope.launch {
                    playlist.takeIf { it.size > 0 }?.run {
                        launch((Dispatchers.Main)) {
                            val task = (requireActivity().application as MainApplication)
                                .getAlbumPictureAsync(currentTrack.path)

                            playlistImage.setImageBitmap(task.await())
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(layoutInflater.inflate(R.layout.list_item_playlist, parent, false))

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(playlists[position])
    }
}