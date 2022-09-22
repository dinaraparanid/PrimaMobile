package com.dinaraparanid.prima.fragments.track_lists

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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentSelectTrackListBinding
import com.dinaraparanid.prima.databinding.ListItemSelectTrackBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.TrackListSearchFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.setMainLabelInitializedSync
import com.dinaraparanid.prima.viewmodels.mvvm.TrackListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*
import com.dinaraparanid.prima.viewmodels.androidx.TrackSelectViewModel as AndroidXTrackSelectViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackSelectViewModel as MVVMTrackSelectViewModel

/** [TrackListSearchFragment] for track selection when adding to playlist */

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
    private val awaitAdapterInitCondition = AsyncCondVar()

    override val viewModel by lazy {
        ViewModelProvider(this)[AndroidXTrackSelectViewModel::class.java]
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
        private const val NEW_SET_KEY = "new_set"
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
        private var gtmPlaybackLength: Byte = 0

        internal fun setPlaylistId(playlistId: Long): Builder {
            this.playlistId = playlistId
            return this
        }

        internal fun setGTMPlaybackLength(gtmPlaybackLength: Byte): Builder {
            this.gtmPlaybackLength = gtmPlaybackLength
            return this
        }

        /** Creates new [TrackSelectFragment] with args given to the builder */
        internal fun build() = TrackSelectFragment().also {
            it.arguments = Bundle().apply {
                putLong(PLAYLIST_ID_KEY, playlistId)
                putSerializable(PLAYLIST_TRACKS_KEY, playlistTracks)
                putInt(TRACKS_SELECTION_TARGET, target.ordinal)
                putByte(PLAYBACK_LENGTH_KEY, gtmPlaybackLength)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.tracks)
        tracksSelectionTarget = TracksSelectionTarget.values()[
                requireArguments().getInt(TRACKS_SELECTION_TARGET)
        ]

        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)

        runOnIOThread {
            loadAsync().join()

            launch(Dispatchers.Main) {
                initAdapter()
                setEmptyTextViewVisibility(itemList)
                amountOfTracks!!.text = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                adapter.setCurrentList(itemList)
            }

            itemListSearch.addAll(itemList)
        }

        playlistTracks.addAll((requireArguments().getSerializable(PLAYLIST_TRACKS_KEY) as Array<out AbstractTrack>))
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
        playbackLength = requireArguments().getByte(PLAYBACK_LENGTH_KEY)

        viewModel.load(
            savedInstanceState
                ?.getSerializable(NEW_SET_KEY) as Array<AbstractTrack>?
                ?: playlistTracks.toTypedArray()
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

                amountOfTracks = selectAmountOfTracks.apply { isSelected = true }
                trackOrderTitle = selectTrackOrderTitle
                updateOrderTitle()

                emptyTextView = selectTracksEmpty
                setEmptyTextViewVisibility(itemList)

                runOnIOThread {
                    recyclerView = selectTrackRecyclerView.apply {
                        while (!isAdapterInit)
                            awaitAdapterInitCondition.blockAsync()

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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_select, menu)
        (menu.findItem(R.id.select_find).actionView as SearchView)
            .setOnQueryTextListener(this@TrackSelectFragment)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.select_find_by -> selectSearch()

            R.id.accept_selected_items -> when (tracksSelectionTarget) {
                TracksSelectionTarget.CUSTOM -> runOnIOThread {
                    val task = CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getPlaylistAsync(playlistId)

                    val oldTracks = playlistTracks.toHashSet()

                    val removes = async(Dispatchers.IO) {
                        oldTracks
                            .filter { it !in viewModel.newSetFlow.value }
                            .map {
                                CustomPlaylistsRepository
                                    .getInstanceSynchronized()
                                    .removeTrackAsync(it.path, playlistId)
                            }
                    }

                    val playlist = task.await()!!

                    val adds = async(Dispatchers.IO) {
                        viewModel.newSetFlow.value
                            .filter { it !in oldTracks }
                            .map {
                                CustomPlaylistsRepository.getInstanceSynchronized()
                                    .addTracksAsync(
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
                                            it.addDate,
                                            it.trackNumberInAlbum
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
                    .newSetFlow
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
                viewModel.newSetFlow.value.addAll(itemListSearch.tracks)
                runOnUIThread { updateUIAsync(itemListSearch, isLocking = true) }
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(
            NEW_SET_KEY,
            viewModel.newSetFlow.value.toTypedArray()
        )

        super.onSaveInstanceState(outState)
    }

    /** Updates UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: List<Pair<Int, AbstractTrack>>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)

        val text = "${resources.getString(R.string.tracks)}: ${src.size}"
        amountOfTracks!!.text = text
    }

    /** Loads all tracks from [MediaStore] */
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
                        Params.Companion.TracksOrder.POS_IN_ALBUM -> MediaStore.Audio.Media.TRACK
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
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.TRACK
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
                    if (cursor != null)
                        application.addTracksFromStoragePaired(cursor, itemList)
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }

    /** Initializes adapter */
    override fun initAdapter() {
        _adapter = TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        isAdapterInit = true
        runOnUIThread { awaitAdapterInitCondition.openAsync() }
    }

    inner class TrackAdapter :
        AsyncListDifferAdapter<Pair<Int, AbstractTrack>, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(
            first: Pair<Int, AbstractTrack>,
            second: Pair<Int, AbstractTrack>
        ) = first.first == second.first && first.second == second.second

        inner class TrackHolder(private val trackBinding: ListItemSelectTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) = Unit

            internal fun bind(track: AbstractTrack) {
                trackBinding.viewModel = MVVMTrackSelectViewModel(
                    track,
                    this@TrackSelectFragment.viewModel
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackHolder(
            ListItemSelectTrackBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: TrackHolder, position: Int) =
            holder.bind(differ.currentList[position].second)
    }
}