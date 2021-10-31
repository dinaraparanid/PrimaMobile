package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectPlaylistBinding
import com.dinaraparanid.prima.databinding.ListItemSelectPlaylistBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.viewmodels.androidx.PlaylistSelectedViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistSelectViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.*

/**
 * [ListFragment] to select playlist when adding track
 */

class PlaylistSelectFragment :
    UpdatingListFragment<String,
            PlaylistSelectFragment.PlaylistAdapter,
            PlaylistSelectFragment.PlaylistAdapter.PlaylistHolder,
            FragmentSelectPlaylistBinding>() {
    private val playlistList = mutableListOf<String>()
    private lateinit var track: AbstractTrack

    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentSelectPlaylistBinding? = null
    override var adapter: PlaylistAdapter? = PlaylistAdapter(listOf())
    override var emptyTextView: TextView? = null

    override val viewModel: PlaylistSelectedViewModel by lazy {
        ViewModelProvider(this)[PlaylistSelectedViewModel::class.java]
    }

    internal companion object {
        private const val TRACK_KEY = "track"
        private const val PLAYLISTS_KEY = "playlists"
        private const val SELECT_ALL_KEY = "select_all"
        private const val ADD_SET_KEY = "add_set"
        private const val REMOVE_SET_KEY = "remove_set"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param track track to add to selected playlists
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            track: AbstractTrack,
            playlists: CustomPlaylist.Entity.EntityList
        ) = PlaylistSelectFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putSerializable(TRACK_KEY, track)
                putSerializable(PLAYLISTS_KEY, playlists)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        titleDefault = resources.getString(R.string.playlists)
        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText = resources.getString(R.string.playlists)

        runOnIOThread {
            val task = loadAsync()
            val progress = async(Dispatchers.Main) {
                createAndShowAwaitDialog(requireContext(), false)
            }

            task.join()
            launch(Dispatchers.Main) { progress.await().dismiss() }

            try {
                launch(Dispatchers.Main) { setEmptyTextViewVisibility(itemList) }
            } catch (ignored: Exception) {
                // not initialized
            }

            itemListSearch.addAll(itemList)
            adapter = PlaylistAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        }

        playlistList.addAll(
            (requireArguments().getSerializable(PLAYLISTS_KEY) as CustomPlaylist.Entity.EntityList)
                .entities.map { it.title }
        )

        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack

        viewModel.load(
            savedInstanceState?.getBoolean(SELECT_ALL_KEY),
            savedInstanceState?.getSerializable(ADD_SET_KEY) as Array<String>?,
            savedInstanceState?.getSerializable(REMOVE_SET_KEY) as Array<String>?
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
                            val task = loadAsync()
                            val progress = async(Dispatchers.Main) {
                                createAndShowAwaitDialog(requireContext(), false)
                            }

                            task.join()

                            launch(Dispatchers.Main) {
                                progress.await().dismiss()
                                updateUIAsync()
                                isRefreshing = false
                            }
                        }
                    }
                }

                emptyTextView = selectPlaylistEmpty
                setEmptyTextViewVisibility(itemList)

                recyclerView = selectPlaylistRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = this@PlaylistSelectFragment.adapter?.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }

                    addItemDecoration(VerticalSpaceItemDecoration(30))
                }
            }

        if (application.playingBarIsVisible) up()
        fragmentActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            SELECT_ALL_KEY,
            viewModel.isAllSelectedFlow.value
        )

        outState.putSerializable(
            ADD_SET_KEY,
            viewModel.addSetFlow.value.toTypedArray()
        )

        outState.putSerializable(
            REMOVE_SET_KEY,
            viewModel.removeSetFlow.value.toTypedArray()
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
                runOnIOThread {
                    val removes = async(Dispatchers.IO) {
                        viewModel.removeSetFlow.value.map {
                            val task = CustomPlaylistsRepository.instance
                                .getPlaylistAsync(it)

                            CustomPlaylistsRepository.instance.removeTrackAsync(
                                track.path,
                                task.await()!!.id
                            )
                        }
                    }

                    val adds = async(Dispatchers.IO) {
                        viewModel.addSetFlow.value.map {
                            val task = CustomPlaylistsRepository.instance
                                .getPlaylistAsync(it)

                            CustomPlaylistsRepository.instance.addTrackAsync(
                                CustomPlaylistTrack(
                                    track.androidId,
                                    0,
                                    track.title,
                                    track.artist,
                                    track.playlist,
                                    task.await()!!.id,
                                    track.path,
                                    track.duration,
                                    track.relativePath,
                                    track.displayName,
                                    track.addDate
                                )
                            )
                        }
                    }

                    launch(Dispatchers.IO) {
                        val progressDialog = async(Dispatchers.Main) {
                            createAndShowAwaitDialog(requireContext(), false)
                        }

                        removes.await().joinAll()
                        adds.await().joinAll()

                        launch(Dispatchers.Main) {
                            progressDialog.await().dismiss()
                        }

                        fragmentActivity.run {
                            supportFragmentManager.popBackStack()
                            currentFragment.get()?.let {
                                if (it is AbstractTrackListFragment<*>) it.updateUIAsync()
                            }
                        }
                    }
                }
            }

            R.id.select_all -> {
                when {
                    viewModel.isAllSelectedFlow.value -> {
                        viewModel.removeSetFlow.value.apply {
                            addAll(viewModel.addSetFlow.value)
                            addAll(playlistList)
                        }

                        viewModel.addSetFlow.value.clear()
                    }

                    else -> {
                        viewModel.removeSetFlow.value.clear()
                        viewModel.addSetFlow.value.addAll(itemList)
                    }
                }

                viewModel.isAllSelectedFlow.value = !viewModel.isAllSelectedFlow.value
                runOnUIThread { updateUIAsync() }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistList.clear()
    }

    override fun onResume() {
        super.onResume()
        runOnIOThread {
            val task = loadAsync()
            val progress = async(Dispatchers.Main) {
                createAndShowAwaitDialog(requireContext(), false)
            }

            task.join()

            launch(Dispatchers.Main) {
                progress.await().dismiss()
                updateUIAsync()
            }
        }
    }

    override suspend fun updateUIAsync(src: List<String>) = coroutineScope {
        launch(Dispatchers.Main) {
            adapter = PlaylistAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView!!.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    override fun filter(models: Collection<String>?, query: String): List<String> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            if (application.checkAndRequestPermissions()) {
                val task = CustomPlaylistsRepository.instance.getPlaylistsAsync()
                itemList.clear()
                itemList.addAll(task.await().map { it.title })
                Unit
            }
        }
    }

    /**
     * [RecyclerView.Adapter] for [PlaylistSelectFragment]
     */

    inner class PlaylistAdapter(private val playlists: List<String>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {

        /**
         * Set of playlists titles.
         * Helps to optimize search
         */

        internal val playlistSet: Set<String> by lazy {
            playlistList.toSet()
        }

        /**
         * [RecyclerView.ViewHolder] for playlists of [PlaylistAdapter]
         */

        inner class PlaylistHolder(private val playlistBinding: ListItemSelectPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = Unit

            /**
             * Constructs GUI for playlist item
             * @param title playlist's title
             */

            fun bind(title: String): Unit = playlistBinding.run {
                viewModel = PlaylistSelectViewModel(
                    title,
                    this@PlaylistSelectFragment.viewModel,
                    playlistSet,
                    playlistBinding.playlistSelectorButton
                )

                this.title = title
                executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(
                ListItemSelectPlaylistBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(playlists[position])
    }
}