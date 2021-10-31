package com.dinaraparanid.prima.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectTrackListBinding
import com.dinaraparanid.prima.databinding.ListItemSelectTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectedViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackSelectViewModel
import kotlinx.coroutines.*

/**
 * [ListFragment] for track selection when adding to playlist
 */

class TrackSelectFragment :
    TrackListSearchFragment<AbstractTrack,
            TrackSelectFragment.TrackAdapter,
            TrackSelectFragment.TrackAdapter.TrackHolder,
            FragmentSelectTrackListBinding>() {
    private lateinit var tracksSelectionTarget: TracksSelectionTarget
    private val playlistTracks = mutableListOf<AbstractTrack>()
    private var playlistId = 0L
    private var playbackLength: Byte = 0

    override val viewModel: TrackSelectedViewModel by lazy {
        ViewModelProvider(this)[TrackSelectedViewModel::class.java]
    }

    override var updater: SwipeRefreshLayout? = null
    override var adapter: TrackAdapter? = TrackAdapter(mutableListOf())
    override var binding: FragmentSelectTrackListBinding? = null

    override var amountOfTracks: carbon.widget.TextView? = null
    override var trackOrderTitle: carbon.widget.TextView? = null
    override var emptyTextView: TextView? = null

    internal companion object {
        internal enum class TracksSelectionTarget { CUSTOM, GTM }

        private const val PLAYLIST_ID_KEY = "playlist_id"
        private const val PLAYLIST_TRACKS_KEY = "playlist_tracks"
        private const val PLAYBACK_LENGTH_KEY = "playback_length"
        private const val TRACKS_SELECTION_TARGET = "tracks_selection_target"
        private const val SELECT_ALL_KEY = "select_all"
        private const val ADD_SET_KEY = "add_set"
        private const val REMOVE_SET_KEY = "remove_set"
    }

    /**
     * Creates new instance of fragment with params
     * @param mainLabelOldText old main label text (to return)
     * @param target creation target
     * @param playlistTracks tracks of playlist if there are any
     * @return new instance of fragment with params in bundle
     */

    internal class Builder(
        private val mainLabelOldText: String,
        private val target: TracksSelectionTarget,
        private vararg val playlistTracks: AbstractTrack
    ) {
        private var playlistId = 0L
        private var playbackLength: Byte = 0

        internal fun setPlaylistId(playlistId: Long): Builder {
            this.playlistId = playlistId
            return this
        }

        internal fun setPlaybackLength(playbackLength: Byte): Builder {
            this.playbackLength = playbackLength
            return this
        }

        internal fun build() = TrackSelectFragment().also {
            it.arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putLong(PLAYLIST_ID_KEY, playlistId)
                putSerializable(PLAYLIST_TRACKS_KEY, playlistTracks)
                putInt(TRACKS_SELECTION_TARGET, target.ordinal)
                putByte(PLAYBACK_LENGTH_KEY, playbackLength)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText = resources.getString(R.string.tracks)
        tracksSelectionTarget = TracksSelectionTarget.values()[requireArguments().getInt(TRACKS_SELECTION_TARGET)]

        runOnIOThread {
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

        playlistTracks.addAll((requireArguments().getSerializable(PLAYLIST_TRACKS_KEY) as Array<AbstractTrack>))
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
        playbackLength = requireArguments().getByte(PLAYBACK_LENGTH_KEY)

        viewModel.load(
            savedInstanceState?.getBoolean(SELECT_ALL_KEY),
            savedInstanceState?.getSerializable(ADD_SET_KEY) as Array<AbstractTrack>?,
            savedInstanceState?.getSerializable(REMOVE_SET_KEY) as Array<AbstractTrack>?
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
                        runOnUIThread {
                            itemList.clear()
                            loadAsync().join()
                            updateUIAsync()
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

        if (application.playingBarIsVisible) up()
        fragmentActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            SELECT_ALL_KEY,
            viewModel.selectAllFlow.value
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
        menu.findItem(R.id.select_find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.accept_selected_items -> when (tracksSelectionTarget) {
                TracksSelectionTarget.CUSTOM -> runOnIOThread {
                    val task = CustomPlaylistsRepository.instance.getPlaylistAsync(mainLabelOldText)

                    val removes = async(Dispatchers.IO) {
                        viewModel.removeSetFlow.value.map {
                            CustomPlaylistsRepository.instance.removeTrackAsync(
                                it.path,
                                playlistId
                            )
                        }
                    }

                    val id = task.await()!!.id

                    val adds = async(Dispatchers.IO) {
                        viewModel.addSetFlow.value.map {
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
                                if (it is CustomPlaylistTrackListFragment) it.updateUIAsync()
                            }
                        }
                    }
                }

                TracksSelectionTarget.GTM -> viewModel
                    .addSetFlow
                    .value
                    .toPlaylist()
                    .takeIf { it.size > 3 }
                    ?.let {
                        requireActivity().run {
                            startActivity(
                                Intent(
                                    requireContext().applicationContext,
                                    GuessTheMelodyActivity::class.java
                                ).apply {
                                    putExtra(
                                        GuessTheMelodyActivity.PLAYLIST_KEY,
                                        it.shuffled().toTypedArray()
                                    )

                                    putExtra(
                                        GuessTheMelodyActivity.MAX_PLAYBACK_LENGTH_KEY,
                                        playbackLength
                                    )
                                }
                            )

                            repeat(2) { supportFragmentManager.popBackStack() }
                        }
                    } ?: Toast
                    .makeText(requireContext(), R.string.game_playlist_small, Toast.LENGTH_LONG)
                    .show()
            }

            R.id.select_all -> {
                when {
                    viewModel.selectAllFlow.value -> {
                        viewModel.removeSetFlow.value.apply {
                            addAll(viewModel.addSetFlow.value)
                            addAll(playlistTracks)
                        }

                        viewModel.addSetFlow.value.clear()
                    }

                    else -> {
                        viewModel.removeSetFlow.value.clear()
                        viewModel.addSetFlow.value.addAll(itemListSearch.filter { it !in playlistTracks })
                    }
                }

                viewModel.selectAllFlow.value = !viewModel.selectAllFlow.value
                runOnUIThread { updateUIAsync(itemListSearch) }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override suspend fun updateUIAsync(src: List<AbstractTrack>) = coroutineScope {
        launch(Dispatchers.Main) {
            adapter = TrackAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            recyclerView!!.adapter = adapter
            setEmptyTextViewVisibility(src)

            val text = "${resources.getString(R.string.tracks)}: ${src.size}"
            amountOfTracks!!.text = text
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
                        application.addTracksFromStorage(cursor, itemList)

                    updateUIAsync()
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }

    inner class TrackAdapter(private val tracks: List<AbstractTrack>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        internal val tracksSet: Set<String> by lazy {
            playlistTracks.map(AbstractTrack::path).toSet()
        }

        inner class TrackHolder(private val trackBinding: ListItemSelectTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = Unit

            fun bind(track: AbstractTrack, ind: Int) {
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