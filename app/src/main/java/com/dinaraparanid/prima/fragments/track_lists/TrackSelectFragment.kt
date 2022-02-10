package com.dinaraparanid.prima.fragments.track_lists

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ConditionVariable
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectTrackListBinding
import com.dinaraparanid.prima.databinding.ListItemSelectTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectedViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackSelectViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*

/** [ListFragment] for track selection when adding to playlist */

class TrackSelectFragment :
    TrackListSearchFragment<AbstractTrack,
            TrackSelectFragment.TrackAdapter,
            TrackSelectFragment.TrackAdapter.TrackHolder,
            FragmentSelectTrackListBinding>() {
    private lateinit var tracksSelectionTarget: TracksSelectionTarget
    private val playlistTracks = mutableListOf<AbstractTrack>()
    private var playlistId = 0L
    private var playbackLength: Byte = 0
    private var awaitDialog: Deferred<KProgressHUD>? = null

    @Volatile
    private var isAdapterInit = false
    private val awaitAdapterInitCondition = ConditionVariable()

    override val viewModel: TrackSelectedViewModel by lazy {
        ViewModelProvider(this)[TrackSelectedViewModel::class.java]
    }

    override var _adapter: TrackAdapter? = null
    override var updater: SwipeRefreshLayout? = null
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
     * @param target creation target
     * @param playlistTracks tracks of playlist if there are any
     * @return new instance of fragment with params in bundle
     */

    internal class Builder(
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
                putLong(PLAYLIST_ID_KEY, playlistId)
                putSerializable(PLAYLIST_TRACKS_KEY, playlistTracks)
                putInt(TRACKS_SELECTION_TARGET, target.ordinal)
                putByte(PLAYBACK_LENGTH_KEY, playbackLength)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.tracks)
        tracksSelectionTarget = TracksSelectionTarget.values()[requireArguments().getInt(TRACKS_SELECTION_TARGET)]

        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        runOnIOThread {
            loadAsync().join()

            launch(Dispatchers.Main) {
                initAdapter()
                setEmptyTextViewVisibility(itemList)
                adapter.setCurrentList(itemList)
            }

            itemListSearch.addAll(itemList)
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
                            updateUIAsync(isLocking = true)
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

                runOnIOThread {
                    recyclerView = selectTrackRecyclerView.apply {
                        while (!isAdapterInit)
                            awaitAdapterInitCondition.block()

                        launch(Dispatchers.Main) {
                            layoutManager = LinearLayoutManager(context)
                            adapter = this@TrackSelectFragment.adapter.apply {
                                stateRestorationPolicy =
                                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                            }
                            addItemDecoration(VerticalSpaceItemDecoration(30))
                        }
                    }
                }
            }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
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
                    val task = CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getPlaylistAsync(playlistId)

                    val removes = async(Dispatchers.IO) {
                        viewModel.removeSetFlow.value.map {
                            CustomPlaylistsRepository
                                .getInstanceSynchronized()
                                .removeTrackAsync(it.path, playlistId)
                        }
                    }

                    val playlist = task.await()!!

                    val adds = async(Dispatchers.IO) {
                        viewModel.addSetFlow.value.map {
                            CustomPlaylistsRepository.getInstanceSynchronized().addTrackAsync(
                                CustomPlaylistTrack(
                                    it.androidId,
                                    0,
                                    it.title,
                                    it.artist,
                                    it.album,
                                    playlist.id,
                                    playlist.title,
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
                        awaitDialog = async(Dispatchers.Main) {
                            createAndShowAwaitDialog(requireContext(), false)
                        }

                        removes.await().joinAll()
                        adds.await().joinAll()

                        launch(Dispatchers.Main) {
                            awaitDialog?.await()?.dismiss()
                        }

                        fragmentActivity.run {
                            supportFragmentManager.popBackStack()
                            currentFragment.get()?.let {
                                if (it is CustomPlaylistTrackListFragment)
                                    it.updateUIAsync(isLocking = true)
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
                    .makeText(
                        requireContext(),
                        R.string.game_playlist_small,
                        Toast.LENGTH_LONG
                    )
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
                        viewModel.addSetFlow.value.addAll(
                            itemListSearch.filter { it.second !in playlistTracks }.tracks
                        )
                    }
                }

                viewModel.selectAllFlow.value = !viewModel.selectAllFlow.value
                runOnUIThread { updateUIAsync(itemListSearch, isLocking = true) }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override suspend fun updateUIAsyncNoLock(src: List<Pair<Int, AbstractTrack>>) {


        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)

        val text = "${resources.getString(R.string.tracks)}: ${src.size}"
        amountOfTracks!!.text = text
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
                val order = "${
                    when (Params.getInstanceSynchronized().tracksOrder.first) {
                        Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                        Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                        Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                        Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                    }
                } ${if (Params.getInstanceSynchronized().tracksOrder.second) "ASC" else "DESC"}"

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
                        application.addTracksFromStoragePaired(cursor, itemList)
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }

    override fun initAdapter() {
        _adapter = TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        isAdapterInit = true
        awaitAdapterInitCondition.open()
    }

    inner class TrackAdapter : AsyncListDifferAdapter<Pair<Int, AbstractTrack>, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(
            first: Pair<Int, AbstractTrack>,
            second: Pair<Int, AbstractTrack>
        ) = first.first == second.first && first.second == second.second

        internal val tracksSet by lazy {
            playlistTracks.map(AbstractTrack::path).toSet()
        }

        inner class TrackHolder(private val trackBinding: ListItemSelectTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = Unit

            fun bind(track: AbstractTrack) {
                trackBinding.viewModel = TrackSelectViewModel(
                    track,
                    this@TrackSelectFragment.viewModel,
                    tracksSet,
                    trackBinding.trackSelectorButton
                )

                if (Params.instance.areCoversDisplayed)
                    runOnUIThread {
                        try {
                            val albumImage = trackBinding.selectTrackAlbumImage
                            val task = application.getAlbumPictureAsync(track.path)

                            Glide.with(this@TrackSelectFragment)
                                .load(task.await())
                                .placeholder(R.drawable.album_default)
                                .skipMemoryCache(true)
                                .thumbnail(0.5F)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .override(albumImage.width, albumImage.height)
                                .into(albumImage)
                        } catch (ignored: Exception) {
                            // Image is to big to show
                        }
                    }

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

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(differ.currentList[position].second)
    }
}