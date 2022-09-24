package com.dinaraparanid.prima.fragments.track_collections

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectPlaylistBinding
import com.dinaraparanid.prima.databinding.ListItemSelectPlaylistBinding
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivityUpdatingListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.setMainLabelInitializedSync
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.*
import com.dinaraparanid.prima.viewmodels.androidx.PlaylistSelectViewModel as AndroidXPlaylistSelectViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistSelectViewModel as MVVMPlaylistSelectViewModel

/** [MainActivityUpdatingListFragment] to select playlist when adding track */

class PlaylistSelectFragment : MainActivityUpdatingListFragment<
        CustomPlaylist.Entity,
        PlaylistSelectFragment.PlaylistAdapter,
        PlaylistSelectFragment.PlaylistAdapter.PlaylistHolder,
        FragmentSelectPlaylistBinding>() {
    private lateinit var track: AbstractTrack
    private val playlistList = mutableListOf<CustomPlaylist.Entity>()

    @Volatile
    private var isAdapterInit = false
    private val awaitAdapterInitCondition = AsyncCondVar()

    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentSelectPlaylistBinding? = null
    override var emptyTextView: TextView? = null
    override var _adapter: PlaylistAdapter? = null

    override val viewModel by lazy {
        ViewModelProvider(this)[AndroidXPlaylistSelectViewModel::class.java]
    }

    internal companion object {
        private const val TRACK_KEY = "track"
        private const val PLAYLISTS_KEY = "playlists"
        private const val NEW_SET_KEY = "new_set"

        /**
         * Creates new instance of fragment with params
         * @param track track to add to selected playlists
         * @param playlists list of all user's playlists
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            track: AbstractTrack,
            playlists: Array<CustomPlaylist.Entity>
        ) = PlaylistSelectFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TRACK_KEY, track)
                putSerializable(PLAYLISTS_KEY, playlists)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(resources.getString(R.string.playlists))
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)

        runOnIOThread {
            val task = loadAsync()
            task.join()

            try {
                launch(Dispatchers.Main) { setEmptyTextViewVisibility(itemList) }
            } catch (ignored: Exception) {
                // not initialized
            }

            initAdapter()
            awaitAdapterInitCondition.openAsync()
            itemListSearch.addAll(itemList)
            adapter.setCurrentList(itemList)
        }

        playlistList.addAll(requireArguments().getSerializable(PLAYLISTS_KEY) as Array<out CustomPlaylist.Entity>)
        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack

        viewModel.load(
            savedInstanceState
                ?.getSerializable(NEW_SET_KEY) as Array<CustomPlaylist.Entity>?
                ?: playlistList.toTypedArray()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentSelectPlaylistBinding>(
                inflater,
                R.layout.fragment_select_playlist,
                container,
                false
            )
            .apply {
                viewModel = ViewModel()

                updater = selectPlaylistSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnIOThread {
                            loadAsync().join()
                            launch(Dispatchers.Main) {
                                setColorSchemeColors(Params.getInstanceSynchronized().primaryColor)
                                updateUIAsync(isLocking = true)
                                isRefreshing = false
                            }
                        }
                    }
                }

                emptyTextView = selectPlaylistEmpty
                setEmptyTextViewVisibility(itemList)

                recyclerView = selectPlaylistRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)

                    runOnUIThread {
                        while (!isAdapterInit)
                            awaitAdapterInitCondition.blockAsync()

                        adapter = this@PlaylistSelectFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                    }
                }
            }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_select, menu)
        (menu.findItem(R.id.select_find).actionView as SearchView)
            .setOnQueryTextListener(this@PlaylistSelectFragment)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.accept_selected_items -> {
                runOnIOThread {
                    val oldPlaylists = playlistList.toHashSet()

                    val removes = async(Dispatchers.IO) {
                        oldPlaylists
                            .filter { it !in viewModel.newSetFlow.value }
                            .map {
                                val task = CustomPlaylistsRepository
                                    .getInstanceSynchronized()
                                    .getPlaylistAsync(it.title)

                                CustomPlaylistsRepository
                                    .getInstanceSynchronized()
                                    .removeTrackAsync(track.path, task.await()!!.id)
                            }
                    }

                    val adds = async(Dispatchers.IO) {
                        viewModel.newSetFlow.value
                            .filter { it !in oldPlaylists }
                            .map {
                                val playlist = CustomPlaylistsRepository
                                    .getInstanceSynchronized()
                                    .getPlaylistAsync(it.title)
                                    .await()!!

                                CustomPlaylistsRepository.getInstanceSynchronized()
                                    .addTracksAsync(
                                        CustomPlaylistTrack(
                                            track.androidId,
                                            0,
                                            track.title,
                                            track.artist,
                                            track.album,
                                            playlist.id,
                                            playlist.title,
                                            track.path,
                                            track.duration,
                                            track.relativePath,
                                            track.displayName,
                                            track.addDate,
                                            track.trackNumberInAlbum
                                        )
                                    )
                            }
                    }

                    launch(Dispatchers.IO) {
                        removes.await().joinAll()
                        adds.await().joinAll()

                        launch(Dispatchers.Main) {
                            fragmentActivity.run {
                                supportFragmentManager.popBackStack()
                                currentFragment.get()?.let {
                                    if (it is AbstractTrackListFragment<*>)
                                        it.updateUIAsync(isLocking = true)
                                }
                            }
                        }
                    }
                }
            }

            R.id.select_all -> {
                viewModel.newSetFlow.value.addAll(itemListSearch)
                runOnUIThread { updateUIAsync(isLocking = true) }
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(
            NEW_SET_KEY,
            viewModel.newSetFlow.value.toTypedArray()
        )

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistList.clear()
    }

    override fun onResume() {
        super.onResume()
        runOnIOThread {
            loadAsync().join()
            launch(Dispatchers.Main) { updateUIAsync(isLocking = true) }
        }
    }

    /** Updates UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: List<CustomPlaylist.Entity>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /** Filters all playlists by [query] (playlist's title must contains [query]) */
    override fun filter(models: Collection<CustomPlaylist.Entity>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    /** Loads all custom playlists */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val task = CustomPlaylistsRepository
                .getInstanceSynchronized()
                .getPlaylistsAsync()

            itemList.clear()
            itemList.addAll(task.await())
        }
    }

    override fun initAdapter() {
        _adapter = PlaylistAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        isAdapterInit = true
    }

    /** [RecyclerView.Adapter] for [PlaylistSelectFragment] */
    inner class PlaylistAdapter :
        AsyncListDifferAdapter<CustomPlaylist.Entity, PlaylistAdapter.PlaylistHolder>() {
        override fun areItemsEqual(
            first: CustomPlaylist.Entity,
            second: CustomPlaylist.Entity
        ) = first.id == second.id

        /** Set of playlists titles optimizes search */
        internal val playlistSet by lazy { playlistList.toHashSet() }

        /** [RecyclerView.ViewHolder] for playlists of [PlaylistAdapter] */
        inner class PlaylistHolder(private val playlistBinding: ListItemSelectPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = Unit

            /**
             * Constructs GUI for playlist item
             * @param playlist playlist itself
             */

            internal fun bind(playlist: CustomPlaylist.Entity) = playlistBinding.run {
                viewModel = MVVMPlaylistSelectViewModel(
                    playlist,
                    this@PlaylistSelectFragment.viewModel
                )

                if (Params.instance.areCoversDisplayed)
                    runOnIOThread {
                        try {
                            val taskDB = CoversRepository
                                .getInstanceSynchronized()
                                .getPlaylistWithCoverAsync(playlist.title)
                                .await()

                            when {
                                taskDB != null -> launch(Dispatchers.Main) {
                                    Glide.with(this@PlaylistSelectFragment)
                                        .load(taskDB.image.toBitmap())
                                        .placeholder(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(playlistImage.width, playlistImage.height)
                                        .into(playlistImage)
                                }

                                else -> {
                                    CustomPlaylistsRepository
                                        .getInstanceSynchronized()
                                        .getFirstTrackOfPlaylistAsync(playlist.title)
                                        .await()
                                        ?.let {
                                            launch(Dispatchers.Main) {
                                                Glide.with(this@PlaylistSelectFragment)
                                                    .load(
                                                        application
                                                            .getAlbumPictureAsync(it.path)
                                                            .await()
                                                    )
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
                                }
                            }
                        } catch (e: Exception) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.image_too_big,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlaylistHolder(
            ListItemSelectPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}