package com.dinaraparanid.prima.fragments

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectTrackListBinding
import com.dinaraparanid.prima.databinding.ListItemSelectTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.ListFragment
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectedViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackSelectViewModel
import kotlinx.coroutines.*

/**
 * [ListFragment] for track selection when adding to playlist
 */

class TrackSelectFragment :
    TrackListSearchFragment<Track, TrackSelectFragment.TrackAdapter.TrackHolder>() {
    private val playlistTracks = mutableListOf<Track>()
    private var playlistId = 0L

    override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? =
        TrackAdapter(mutableListOf())

    override val viewModel: TrackSelectedViewModel by lazy {
        ViewModelProvider(this)[TrackSelectedViewModel::class.java]
    }

    private lateinit var binding: FragmentSelectTrackListBinding

    override lateinit var amountOfTracks: carbon.widget.TextView
    override lateinit var trackOrderTitle: carbon.widget.TextView
    override lateinit var emptyTextView: TextView
    override lateinit var updater: SwipeRefreshLayout

    internal companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"
        private const val PLAYLIST_TRACKS_KEY = "playlist_tracks"
        private const val SELECT_ALL_KEY = "select_all"
        private const val ADD_SET_KEY = "add_set"
        private const val REMOVE_SET_KEY = "remove_set"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param playlistId id of playlist
         * @param playlistTracks tracks of playlist if there are any
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            playlistId: Long,
            playlistTracks: Playlist
        ) = TrackSelectFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
                putSerializable(PLAYLIST_TRACKS_KEY, playlistTracks)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            loadAsync().join()

            try {
                launch(Dispatchers.Main) { setEmptyTextViewVisibility(itemList) }
            } catch (ignored: Exception) {
                // not initialized
            }

            itemListSearch.addAll(itemList)
            adapter = TrackAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        }

        playlistTracks.addAll((requireArguments().getSerializable(PLAYLIST_TRACKS_KEY) as Playlist))
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)

        viewModel.load(
            savedInstanceState?.getBoolean(SELECT_ALL_KEY),
            savedInstanceState?.getSerializable(ADD_SET_KEY) as Array<Track>?,
            savedInstanceState?.getSerializable(REMOVE_SET_KEY) as Array<Track>?
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        titleDefault = resources.getString(R.string.tracks)

        binding = DataBindingUtil
            .inflate<FragmentSelectTrackListBinding>(
                inflater,
                R.layout.fragment_select_track_list,
                container,
                false
            )
            .apply {
                viewModel = TrackListViewModel(this@TrackSelectFragment)

                updater = selectTrackSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        this@TrackSelectFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                            itemList.clear()
                            loadAsync().join()
                            updateUI()
                            isRefreshing = false
                        }
                    }
                }

                amountOfTracks = selectAmountOfTracks.apply {
                    isSelected = true
                    val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                    text = txt
                }

                trackOrderTitle = selectTrackOrderTitle
                updateOrderTitle()

                emptyTextView = selectTracksEmpty
                setEmptyTextViewVisibility(itemList)

                recyclerView = selectTrackRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = this@TrackSelectFragment.adapter?.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }
                    addItemDecoration(VerticalSpaceItemDecoration(30))
                }
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).binding.mainLabel.text = mainLabelCurText
        return binding.root
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
        menu.findItem(R.id.select_find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.accept_selected_items -> viewModel.viewModelScope.launch(Dispatchers.IO) {
                val task = CustomPlaylistsRepository.instance.getPlaylistAsync(mainLabelOldText)

                val removes = viewModel.viewModelScope.async(Dispatchers.IO) {
                    viewModel.removeSetLiveData.value!!.map {
                        CustomPlaylistsRepository.instance.removeTrackAsync(
                            it.path,
                            playlistId
                        )
                    }
                }

                val id = task.await()!!.id

                val adds = viewModel.viewModelScope.async(Dispatchers.IO) {
                    viewModel.addSetLiveData.value!!.map {
                        CustomPlaylistsRepository.instance.addTrackAsync(
                            CustomPlaylistTrack(
                                it.androidId,
                                0,
                                it.title,
                                it.artist,
                                it.playlist,
                                id,
                                it.path,
                                it.duration,
                                it.relativePath,
                                it.displayName,
                                it.addDate
                            )
                        )
                    }
                }

                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val progressDialog = viewModel.viewModelScope.async(Dispatchers.Main) {
                        createAwaitDialog(requireContext())
                    }

                    removes.await().joinAll()
                    adds.await().joinAll()

                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        progressDialog.await().dismiss()
                    }

                    (requireActivity() as MainActivity).run {
                        supportFragmentManager.popBackStack()
                        currentFragment?.let {
                            if (it is CustomPlaylistTrackListFragment) it.updateUI()
                        }
                    }
                }
            }

            R.id.select_all -> {
                when {
                    viewModel.selectAllLiveData.value!! -> {
                        viewModel.removeSetLiveData.value!!.apply {
                            addAll(viewModel.addSetLiveData.value!!)
                            addAll(playlistTracks)
                        }

                        viewModel.addSetLiveData.value!!.clear()
                    }

                    else -> {
                        viewModel.removeSetLiveData.value!!.clear()
                        viewModel.addSetLiveData.value!!.addAll(itemListSearch.filter { it !in playlistTracks })
                    }
                }

                viewModel.selectAllLiveData.value = !viewModel.selectAllLiveData.value!!
                updateUI(itemListSearch)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun updateUI(src: List<Track>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = TrackAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            recyclerView.adapter = adapter
            setEmptyTextViewVisibility(src)

            val text = "${resources.getString(R.string.tracks)}: ${src.size}"
            amountOfTracks.text = text
        }
    }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
                val order = "${
                    when (Params.instance.tracksOrder.first) {
                        Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                        Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                        Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                        Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                    }
                } ${if (Params.instance.tracksOrder.second) "ASC" else "DESC"}"

                val projection = mutableListOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection.toTypedArray(),
                    selection,
                    null,
                    order
                ).use { cursor ->
                    itemList.clear()

                    if (cursor != null)
                        (requireActivity().application as MainApplication)
                            .addTracksFromStorage(cursor, itemList)
                    updateUI()
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        internal val tracksSet: Set<String> by lazy {
            playlistTracks.map { it.path }.toSet()
        }

        inner class TrackHolder(private val trackBinding: ListItemSelectTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = Unit

            fun bind(track: Track, ind: Int) {
                trackBinding.track = track
                trackBinding.viewModel = TrackSelectViewModel(
                    ind + 1,
                    track,
                    this@TrackSelectFragment.viewModel,
                    tracksSet,
                    trackBinding.trackSelectorButton
                )
                trackBinding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                ListItemSelectTrackBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(tracks[position], position)
    }
}