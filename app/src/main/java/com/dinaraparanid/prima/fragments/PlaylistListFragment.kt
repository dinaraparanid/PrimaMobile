package com.dinaraparanid.prima.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentPlaylistsBinding
import com.dinaraparanid.prima.databinding.ListItemPlaylistBinding
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.androidx.PlaylistListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*

/**
 * [ListFragment] for all albums and user's playlists
 */

class PlaylistListFragment :
    UpdatingListFragment<Playlist, PlaylistListFragment.PlaylistAdapter.PlaylistHolder>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Calls new [TypicalTrackListFragment] with playlist's (album's) tracks
         * @param id id of playlist or 0 if it's album
         * @param title title of playlist or album
         */

        fun onPlaylistSelected(
            id: Long,
            title: String
        )
    }

    override var adapter: RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[PlaylistListViewModel::class.java]
    }

    private lateinit var binding: FragmentPlaylistsBinding
    private lateinit var mvvmViewModel: com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

    override lateinit var emptyTextView: TextView
    override lateinit var updater: SwipeRefreshLayout

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
    ): View {
        titleDefault = resources.getString(R.string.playlists)

        binding = DataBindingUtil
            .inflate<FragmentPlaylistsBinding>(
                inflater,
                R.layout.fragment_playlists,
                container,
                false
            )
            .apply {
                viewModel =
                    com.dinaraparanid.prima.viewmodels.mvvm.PlaylistListViewModel(this@PlaylistListFragment)

                mvvmViewModel = viewModel!!
                emptyTextView = playlistsEmpty

                updater = playlistSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        this@PlaylistListFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                            loadAsync().join()
                            updateUI()
                            isRefreshing = false
                        }
                    }
                }

                this@PlaylistListFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                    val progress = KProgressHUD.create(requireContext())
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setLabel(resources.getString(R.string.please_wait))
                        .setCancellable(true)
                        .setAnimationSpeed(2)
                        .setDimAmount(0.5F)
                        .show()

                    loadAsync().join()
                    progress.dismiss()

                    itemListSearch.addAll(itemList)
                    adapter = PlaylistAdapter(itemList).apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }

                    setEmptyTextViewVisibility(itemList)

                    recyclerView = playlistRecyclerView.apply {
                        layoutManager = when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_PORTRAIT ->
                                when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                    Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                        GridLayoutManager(context, 2)

                                    Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                        GridLayoutManager(context, 3)

                                    else -> GridLayoutManager(context, 2)
                                }

                            else -> when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                    GridLayoutManager(context, 3)

                                Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                    GridLayoutManager(context, 4)

                                else -> GridLayoutManager(context, 3)
                            }
                        }

                        adapter = this@PlaylistListFragment.adapter?.apply {
                            stateRestorationPolicy =
                                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }

                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        addItemDecoration(HorizontalSpaceItemDecoration(30))
                    }

                    if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
                }
            }

        (requireActivity() as MainActivity).binding.mainLabel.text = mainLabelCurText
        return binding.root
    }

    override fun onStop() {
        (requireActivity() as MainActivity).binding.selectButton.visibility = ImageView.INVISIBLE
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
    }

    override fun onResume() {
        val act = requireActivity() as MainActivity

        act.binding.selectButton.visibility = ImageView.VISIBLE

        if (act.needToUpdate) {
            loadContent()
            act.needToUpdate = false
        }

        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_playlists_menu, menu)
        (menu.findItem(R.id.playlist_search).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun updateUI(src: List<Playlist>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = PlaylistAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    private fun loadContent(): Job = viewModel.viewModelScope.launch(Dispatchers.Main) {
        loadAsync().join()
        itemListSearch.addAll(itemList)
        adapter = PlaylistAdapter(itemListSearch).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        updateUI()
    }

    override fun filter(models: Collection<Playlist>?, query: String): List<Playlist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            when (mainLabelCurText) {
                resources.getString(R.string.playlists) -> itemList.run {
                    val task = CustomPlaylistsRepository.instance.getPlaylistsWithTracksAsync()
                    clear()
                    addAll(
                        task
                            .await()
                            .map { (p, t) ->
                                CustomPlaylist(p).apply {
                                    t.takeIf { it.isNotEmpty() }?.let { add(t.first()) }
                                }
                            }
                    )
                    Unit
                }

                else -> try {
                    requireActivity().contentResolver.query(
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
                } catch (ignored: Exception) {
                    // Permission to storage not given
                }
            }
        }
    }

    /**
     * [RecyclerView.Adapter] for [PlaylistListFragment]
     * @param playlists items of fragment
     */

    inner class PlaylistAdapter(private val playlists: List<Playlist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
        /**
         * [RecyclerView.ViewHolder] for tracks of [PlaylistAdapter]
         */

        inner class PlaylistHolder(private val playlistBinding: ListItemPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            private lateinit var playlist: Playlist

            internal val playlistImage: carbon.widget.ImageView = itemView
                .findViewById<carbon.widget.ImageView>(R.id.playlist_image)
                .apply { if (!Params.instance.isRoundingPlaylistImage) setCornerRadius(0F) }

            init {
                playlistBinding.viewModel = mvvmViewModel
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = (callbacker as Callbacks).onPlaylistSelected(
                when (mainLabelCurText) {
                    resources.getString(R.string.playlists) -> runBlocking {
                        CustomPlaylistsRepository.instance
                            .getPlaylistAsync(playlist.title)
                            .await()!!
                            .id
                    }

                    else -> 0
                },
                playlist.title
            )

            /**
             * Makes all GUI customizations for a playlist
             * @param _playlist playlist to bind
             */

            fun bind(_playlist: Playlist) {
                playlistBinding.title = _playlist.title
                playlistBinding.executePendingBindings()

                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    playlist = _playlist

                    if (Params.instance.isPlaylistsImagesShown)
                        viewModel.viewModelScope.launch {
                            playlist.takeIf { it.size > 0 }?.run {
                                launch((Dispatchers.Main)) {
                                    val taskDB = when (mainLabelCurText) {
                                        resources.getString(R.string.playlists) -> ImageRepository
                                            .instance
                                            .getPlaylistWithImageAsync(playlist.title)
                                            .await()

                                        else -> ImageRepository
                                            .instance
                                            .getAlbumWithImageAsync(playlist.title)
                                            .await()
                                    }

                                    when {
                                        taskDB != null -> Glide.with(this@PlaylistListFragment)
                                            .load(taskDB.image.toBitmap())
                                            .placeholder(R.drawable.album_default)
                                            .skipMemoryCache(true)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .override(playlistImage.width, playlistImage.height)
                                            .into(playlistImage)

                                        else -> {
                                            val task =
                                                (requireActivity().application as MainApplication)
                                                    .getAlbumPictureAsync(currentTrack.path, true)

                                            Glide.with(this@PlaylistListFragment)
                                                .load(task.await())
                                                .placeholder(R.drawable.album_default)
                                                .skipMemoryCache(true)
                                                .transition(DrawableTransitionOptions.withCrossFade())
                                                .override(playlistImage.width, playlistImage.height)
                                                .into(playlistImage)
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(
                ListItemPlaylistBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(playlists[position])

        override fun onViewRecycled(holder: PlaylistHolder) {
            Glide.with(this@PlaylistListFragment).clear(holder.playlistImage)
            super.onViewRecycled(holder)
        }
    }
}