package com.dinaraparanid.prima.fragments.guess_the_melody

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
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectPlaylistBinding
import com.dinaraparanid.prima.databinding.ListItemGtmSelectPlaylistBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.FilterFragment
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*

/**
 * Fragment which chooses playlists
 * to start "Guess the Melody" game
 */

class GTMPlaylistSelectFragment : MainActivityUpdatingListFragment<
        AbstractPlaylist,
        GTMPlaylistSelectFragment.PlaylistAdapter,
        GTMPlaylistSelectFragment.PlaylistAdapter.PlaylistHolder,
        FragmentSelectPlaylistBinding>(),
    FilterFragment<AbstractPlaylist> {
    internal interface Callbacks : CallbacksFragment.Callbacks {

        /**
         * Start game if playlist has GTM type
         * or launches tracks' selection fragment
         * @param playlist playlist in which tracks will be guessed or new GTM playlist
         * @param fragment current [GTMPlaylistSelectFragment]
         */

        fun onPlaylistSelected(playlist: AbstractPlaylist, fragment: GTMPlaylistSelectFragment)
    }

    @Volatile
    private var isAdapterInit = false
    private val awaitAdapterInitCondition = AsyncCondVar()

    private var awaitDialog: Deferred<KProgressHUD>? = null
    override var binding: FragmentSelectPlaylistBinding? = null
    override var emptyTextView: TextView? = null
    override var updater: SwipeRefreshLayout? = null
    override var _adapter: PlaylistAdapter? = null

    override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(resources.getString(R.string.playlists))
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)

        runOnIOThread {
            val task = loadAsync()

            awaitDialog = async(Dispatchers.Main) {
                createAndShowAwaitDialog(requireContext(), false)
            }

            task.join()

            launch(Dispatchers.Main) {
                awaitDialog?.await()?.dismiss()
                setEmptyTextViewVisibility(itemList)
                initAdapter()
                adapter.setCurrentList(itemList)
            }

            itemListSearch.addAll(itemList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentSelectPlaylistBinding>(
            inflater,
            R.layout.fragment_select_playlist,
            container,
            false
        ).apply {
            viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()

            updater = selectPlaylistSwipeRefreshLayout.apply {
                setColorSchemeColors(Params.instance.primaryColor)
                setOnRefreshListener {
                    runOnIOThread {
                        val task = loadAsync()
                        awaitDialog = async(Dispatchers.Main) {
                            createAndShowAwaitDialog(requireContext(), false)
                        }

                        task.join()

                        launch(Dispatchers.Main) {
                            awaitDialog?.await()?.dismiss()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }
            }

            emptyTextView = selectPlaylistEmpty
            setEmptyTextViewVisibility(itemList)

            runOnIOThread {
                recyclerView = selectPlaylistRecyclerView.apply {
                    while (!isAdapterInit)
                        awaitAdapterInitCondition.blockAsync()

                    launch(Dispatchers.Main) {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@GTMPlaylistSelectFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                    }
                }
            }
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@GTMPlaylistSelectFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    /**
     * Filters playlists by query (title must contains query)
     * @param models playlists to filter
     * @param query searched title
     */

    override fun filter(models: Collection<AbstractPlaylist>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    /** Loads all albums and custom playlists */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()

            // New GTM playlist

            itemList.add(
                DefaultPlaylist(
                    resources.getString(R.string.create_playlist),
                    AbstractPlaylist.PlaylistType.GTM
                )
            )

            // Albums

            itemList.addAll(
                application
                    .allTracksWithoutHidden
                    .map { it.album to it }
                    .distinctBy { it.first.trim().lowercase() }
                    .sortedBy(Pair<String, *>::first)
                    .map { (albumTitle, track) ->
                        DefaultPlaylist(
                            albumTitle,
                            AbstractPlaylist.PlaylistType.ALBUM,
                            track
                        )
                    }
            )

            // User's playlists

            itemList.addAll(
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .getPlaylistsAndTracksAsync()
                    .await()
                    .filter { (_, track) -> track != null }
                    .map { (playlist, track) -> CustomPlaylist(playlist.title, track!!) }
            )

            itemList.run {
                val distinctList = distinctBy { "${it.title}${it.type.name}" }
                clear()
                addAll(distinctList)
            }
        }
    }

    /** Updates both adapter and empty text view (if there are no playlists) */
    override suspend fun updateUIAsyncNoLock(src: List<AbstractPlaylist>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /** Initializes recycler view's adapter */
    override fun initAdapter() {
        _adapter = PlaylistAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        isAdapterInit = true
        awaitAdapterInitCondition.open()
    }

    /** [RecyclerView.Adapter] for [PlaylistSelectFragment] */
    inner class PlaylistAdapter : AsyncListDifferAdapter<AbstractPlaylist, PlaylistAdapter.PlaylistHolder>() {
        override fun areItemsEqual(
            first: AbstractPlaylist,
            second: AbstractPlaylist
        ) = first == second

        /** [RecyclerView.ViewHolder] for playlists of [PlaylistAdapter] */
        inner class PlaylistHolder(private val playlistBinding: ListItemGtmSelectPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            private lateinit var playlist: AbstractPlaylist

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) = (callbacker as Callbacks)
                .onPlaylistSelected(playlist, this@GTMPlaylistSelectFragment)

            @Deprecated("All shown albums contain at least one track now")
            private suspend fun getTrackPathFromAlbum() =
                application.getAlbumTracksAsync(playlist.title).await().first().path

            @Deprecated("All shown playlists contain at least one track now")
            private suspend fun getTrackPathFromCustomPlaylist() =
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .getFirstTrackOfPlaylistAsync(playlist.title)
                    .await()
                    ?.path ?: Params.NO_PATH

            /**
             * Constructs GUI for playlist item
             * @param playlist playlist itself
             */

            internal fun bind(playlist: AbstractPlaylist) = playlistBinding.run {
                this@PlaylistHolder.playlist = playlist
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()
                this.title = playlist.title

                if (Params.instance.areCoversDisplayed)
                    runOnIOThread {
                        try {
                            val taskDB = when (playlist.type) {
                                AbstractPlaylist.PlaylistType.CUSTOM -> CoversRepository
                                    .getInstanceSynchronized()
                                    .getPlaylistWithCoverAsync(playlist.title)
                                    .await()

                                AbstractPlaylist.PlaylistType.ALBUM -> CoversRepository
                                    .getInstanceSynchronized()
                                    .getAlbumWithCoverAsync(playlist.title)
                                    .await()

                                AbstractPlaylist.PlaylistType.GTM -> null
                            }

                            when {
                                taskDB != null -> launch(Dispatchers.Main) {
                                    Glide.with(this@GTMPlaylistSelectFragment)
                                        .load(taskDB.image.toBitmap())
                                        .placeholder(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(playlistImage.width, playlistImage.height)
                                        .into(playlistImage)
                                }

                                else -> launch(Dispatchers.Main) {
                                    Glide.with(this@GTMPlaylistSelectFragment)
                                        .run {
                                            when (playlist.type) {
                                                AbstractPlaylist.PlaylistType.GTM -> load(R.drawable.album_default)

                                                else -> load(
                                                    application
                                                        .getAlbumPictureAsync(playlist.currentTrack.path)
                                                        .await()
                                                )
                                            }
                                        }
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
            ListItemGtmSelectPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}