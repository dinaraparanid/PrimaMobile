package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectPlaylistBinding
import com.dinaraparanid.prima.databinding.ListItemGtmSelectPlaylistBinding
import com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.FilterFragment
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
        fun onPlaylistSelected(playlist: AbstractPlaylist, fragment: GTMPlaylistSelectFragment)
    }

    private var awaitDialog: Deferred<KProgressHUD>? = null
    override var binding: FragmentSelectPlaylistBinding? = null
    override var emptyTextView: TextView? = null
    override var updater: SwipeRefreshLayout? = null
    override var _adapter: PlaylistAdapter? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText = resources.getString(R.string.playlists)

        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

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
                            updateUI(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }
            }

            emptyTextView = selectPlaylistEmpty
            setEmptyTextViewVisibility(itemList)

            recyclerView = selectPlaylistRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@GTMPlaylistSelectFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    override fun filter(models: Collection<AbstractPlaylist>?, query: String): List<AbstractPlaylist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()

            // New playlist

            itemList.add(
                DefaultPlaylist(
                    resources.getString(R.string.create_playlist),
                    AbstractPlaylist.PlaylistType.GTM
                )
            )

            // Albums

            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM),
                    null,
                    null,
                    MediaStore.Audio.Media.ALBUM + " ASC"
                ).use { cursor ->
                    if (cursor != null) {
                        val playlistList = mutableListOf<AbstractPlaylist>()

                        while (cursor.moveToNext()) {
                            val albumTitle = cursor.getString(0)

                            application.allTracks
                                .firstOrNull { it.playlist == albumTitle }
                                ?.let { playlistList.add(DefaultPlaylist(albumTitle)) }
                        }

                        itemList.addAll(playlistList.distinctBy(AbstractPlaylist::title))
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }

            // User's playlists

            if (application.checkAndRequestPermissions())
                itemList.addAll(
                    CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getPlaylistsAsync()
                        .await()
                        .map { CustomPlaylist(it) }
                )

            itemList.run {
                val distinctedList = distinctBy { "${it.title}${it.type.name}" }
                clear()
                addAll(distinctedList)
            }
        }
    }

    override suspend fun updateUINoLock(src: List<AbstractPlaylist>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    override fun initAdapter() {
        _adapter = PlaylistAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [PlaylistSelectFragment] */

    inner class PlaylistAdapter : AsyncListDifferAdapter<AbstractPlaylist, PlaylistAdapter.PlaylistHolder>() {
        override fun areItemsEqual(first: AbstractPlaylist, second: AbstractPlaylist) = first == second

        /**
         * [RecyclerView.ViewHolder] for playlists of [PlaylistAdapter]
         */

        inner class PlaylistHolder(private val playlistBinding: ListItemGtmSelectPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            private lateinit var playlist: AbstractPlaylist

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit =
                (callbacker as Callbacks).onPlaylistSelected(playlist, this@GTMPlaylistSelectFragment)

            /**
             * Constructs GUI for playlist item
             * @param playlist playlist itself
             */

            fun bind(playlist: AbstractPlaylist): Unit = playlistBinding.run {
                this@PlaylistHolder.playlist = playlist
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()
                this.title = playlist.title
                executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(
                ListItemGtmSelectPlaylistBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(differ.currentList[position])
    }
}