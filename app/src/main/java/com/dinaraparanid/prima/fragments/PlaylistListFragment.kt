package com.dinaraparanid.prima.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import android.widget.Toast
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
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * [ListFragment] for all albums and user's playlists
 */

class PlaylistListFragment :
    UpdatingListFragment<AbstractPlaylist,
            PlaylistListFragment.PlaylistAdapter,
            PlaylistListFragment.PlaylistAdapter.PlaylistHolder,
            FragmentPlaylistsBinding>() {
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

    override var adapter: PlaylistAdapter? = PlaylistAdapter(listOf())

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentPlaylistsBinding? = null
    private lateinit var mvvmViewModel: com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
    override var emptyTextView: TextView? = null

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
                    com.dinaraparanid.prima.viewmodels.mvvm.PlaylistListViewModel(WeakReference(this@PlaylistListFragment))

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
                    val progress = createAndShowAwaitDialog(requireContext(), false)

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

        (requireActivity() as MainActivity).mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onStop() {
        (requireActivity() as MainActivity).setSelectButtonVisibility(false)
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        Glide.get(requireContext()).clearMemory()
    }

    override fun onResume() {
        (requireActivity() as MainActivity).run {
            setSelectButtonVisibility(true) // А так норм

            if (isUpdateNeeded) {
                loadContent()
                isUpdateNeeded = false
            }
        }

        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_playlists_menu, menu)
        (menu.findItem(R.id.playlist_search).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun updateUI(src: List<AbstractPlaylist>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = PlaylistAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView!!.adapter = adapter
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

    override fun filter(models: Collection<AbstractPlaylist>?, query: String): List<AbstractPlaylist> =
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
                            val playlistList = mutableListOf<AbstractPlaylist>()

                            while (cursor.moveToNext()) {
                                val albumTitle = cursor.getString(0)

                                (requireActivity().application as MainApplication).allTracks
                                    .firstOrNull { it.playlist == albumTitle }
                                    ?.let { track ->
                                        playlistList.add(
                                            DefaultPlaylist(
                                                albumTitle,
                                                AbstractPlaylist.PlaylistType.ALBUM,
                                                track
                                            )
                                        )
                                    }
                            }

                            itemList.addAll(playlistList.distinctBy(AbstractPlaylist::title))
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

    inner class PlaylistAdapter(private val playlists: List<AbstractPlaylist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
        /**
         * [RecyclerView.ViewHolder] for tracks of [PlaylistAdapter]
         */

        inner class PlaylistHolder(private val playlistBinding: ListItemPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            private lateinit var playlist: AbstractPlaylist

            internal val playlistImage: ImageView = itemView
                .findViewById<ImageView>(R.id.playlist_image)
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

            fun bind(_playlist: AbstractPlaylist) {
                playlistBinding.title = _playlist.title
                playlistBinding.executePendingBindings()

                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    playlist = _playlist

                    if (Params.instance.isPlaylistsImagesShown)
                        viewModel.viewModelScope.launch {
                            playlist.takeIf { it.size > 0 }?.run {
                                launch((Dispatchers.Main)) {
                                    try {
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
                                                        .getAlbumPictureAsync(
                                                            currentTrack.path,
                                                            true
                                                        )

                                                Glide.with(this@PlaylistListFragment)
                                                    .load(task.await())
                                                    .placeholder(R.drawable.album_default)
                                                    .skipMemoryCache(true)
                                                    .transition(DrawableTransitionOptions.withCrossFade())
                                                    .override(
                                                        playlistImage.width,
                                                        playlistImage.height
                                                    )
                                                    .into(playlistImage)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.image_too_big,
                                            Toast.LENGTH_LONG
                                        ).show()
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