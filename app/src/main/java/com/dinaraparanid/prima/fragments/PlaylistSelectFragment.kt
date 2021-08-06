package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.ListFragment
import com.dinaraparanid.prima.viewmodels.PlaylistSelectedViewModel
import kotlinx.coroutines.*

/**
 * [ListFragment] to select playlist when adding track
 */

class PlaylistSelectFragment :
    ListFragment<String, PlaylistSelectFragment.PlaylistAdapter.PlaylistHolder>() {
    private val playlistList = mutableListOf<String>()
    private lateinit var track: Track

    override var adapter: RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>? =
        PlaylistAdapter(mutableListOf())

    override val viewModel: PlaylistSelectedViewModel by lazy {
        ViewModelProvider(this)[PlaylistSelectedViewModel::class.java]
    }

    override lateinit var emptyTextView: TextView

    internal companion object {
        private const val TRACK_KEY = "track"
        private const val PLAYLISTS_KEY = "playlists"
        private const val SELECT_ALL_KEY = "select_all"
        private const val ADD_SET_KEY = "add_set"
        private const val REMOVE_SET_KEY = "remove_set"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param track track to add to selected playlists
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            track: Track,
            playlists: CustomPlaylist.Entity.EntityList
        ) = PlaylistSelectFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putSerializable(TRACK_KEY, track)
                putSerializable(PLAYLISTS_KEY, playlists)
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

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().await()
            try {
                setEmptyTextViewVisibility(itemList)
            } catch (ignored: Exception) {
                // not initialized
            }
            itemListSearch.addAll(itemList)
            adapter = PlaylistAdapter(itemList)
        }

        playlistList.addAll(
            (requireArguments().getSerializable(PLAYLISTS_KEY) as CustomPlaylist.Entity.EntityList)
                .entities.map { it.title }
        )

        track = requireArguments().getSerializable(TRACK_KEY) as Track

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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_playlist, container, false)
        titleDefault = resources.getString(R.string.playlists)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.select_playlist_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.instance.theme.rgb)
                setOnRefreshListener {
                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        itemList.clear()
                        loadAsync().await()
                        updateUI()
                        isRefreshing = false
                    }
                }
            }

        val constraintLayout: ConstraintLayout =
            updater.findViewById(R.id.select_playlist_constraint_layout)

        emptyTextView = constraintLayout.findViewById<TextView>(R.id.select_playlist_empty).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }
        setEmptyTextViewVisibility(itemList)

        recyclerView = constraintLayout
            .findViewById<RecyclerView>(R.id.select_playlist_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@PlaylistSelectFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                addItemDecoration(VerticalSpaceItemDecoration(30))
                addItemDecoration(DividerItemDecoration(requireActivity()))
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
                (requireActivity() as MainActivity).run {
                    needToUpdate = true
                    supportFragmentManager.popBackStack()
                }

                viewModel.viewModelScope.apply {
                    launch {
                        viewModel.addSetLiveData.value!!.forEach {
                            launch(Dispatchers.IO) {
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
                                ).await()
                            }
                        }
                    }

                    launch {
                        viewModel.removeSetLiveData.value!!.forEach {
                            launch(Dispatchers.IO) {
                                val task = CustomPlaylistsRepository.instance
                                    .getPlaylistAsync(it)

                                CustomPlaylistsRepository.instance.removeTrack(
                                    track.path,
                                    task.await()!!.id
                                )
                            }
                        }
                    }
                }
            }

            R.id.select_all -> {
                when {
                    viewModel.selectAllLiveData.value!! -> {
                        viewModel.removeSetLiveData.value!!.apply {
                            addAll(viewModel.addSetLiveData.value!!)
                            addAll(playlistList)
                        }

                        viewModel.addSetLiveData.value!!.clear()
                    }

                    else -> {
                        viewModel.removeSetLiveData.value!!.clear()
                        viewModel.addSetLiveData.value!!.addAll(itemList)
                    }
                }

                viewModel.selectAllLiveData.value = !viewModel.selectAllLiveData.value!!
                updateUI()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun updateUI(src: List<String>) {
        adapter = PlaylistAdapter(src).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        recyclerView.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    override fun filter(models: Collection<String>?, query: String): List<String> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            if ((requireActivity().application as MainApplication).checkAndRequestPermissions()) {
                val task = CustomPlaylistsRepository.instance.playlistsAsync
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
        private val click = { title: String, playlistSelector: CheckBox ->
            when {
                playlistSelector.isChecked -> viewModel.addSetLiveData.value!!.add(title)

                else -> when (title) {
                    in viewModel.addSetLiveData.value!! ->
                        viewModel.addSetLiveData.value!!.remove(title)

                    else -> viewModel.removeSetLiveData.value!!.add(title)
                }
            }
        }

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

        inner class PlaylistHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private val titleTextView: TextView = itemView
                .findViewById<TextView>(R.id.select_playlist_title)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            internal val playlistSelector: CheckBox =
                itemView.findViewById(R.id.playlist_selector_button)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = Unit

            /**
             * Constructs GUI for playlist item
             * @param title playlist's title
             */

            fun bind(title: String) {
                titleTextView.text = title
                playlistSelector.isChecked = title !in viewModel.removeSetLiveData.value!!
                        && (title in viewModel.addSetLiveData.value!!
                        || title in playlistSet)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(
                layoutInflater.inflate(
                    R.layout.fragment_item_select_playlist,
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) {
            holder.bind(playlists[position])

            val trackSelector = holder.playlistSelector
            trackSelector.setOnClickListener { click(playlists[position], trackSelector) }
        }
    }
}