package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentCurTrackListBinding
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.extensions.toFormattedTimeString
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.mvvmp.old_shit.CurPlaylistTrackListViewModel
import com.dinaraparanid.prima.mvvmp.old_shit.TrackItemViewModel
import com.dinaraparanid.prima.mvvmp.view.Loader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.util.Collections

/** Current playlist track list fragment with a [BottomSheetBehavior]*/

class CurPlaylistTrackListFragment :
    BottomSheetDialogFragment(),
    AsyncContext,
    UIUpdatable<List<Pair<Int, Track>>>,
    Loader<List<Pair<Int, Track>>>,
    PlayingTrackList<Track> {
    interface Callbacks : CallbacksFragment.CallbackHandler {
        /**
         * Plays track or just shows playing bar
         * @param track track to show in playing bar
         * @param tracks tracks from which current playlist' ll be constructed
         * else it' ll be just shown in playing bar
         */

        fun onTrackSelected(
            track: Track,
            tracks: Collection<Track>,
        )
    }

    private var binding: FragmentCurTrackListBinding? = null
    private var callbacker: Callbacks? = null
    private var recyclerView: RecyclerView? = null
    private var updater: SwipeRefreshLayout? = null
    private var amountOfTracks: carbon.widget.TextView? = null
    private var beforeFragment: WeakReference<Fragment> = WeakReference(null)
    private var awaitDialog: KProgressHUD? = null

    private val itemList: MutableList<Pair<Int, Track>> =
        Collections.synchronizedList(mutableListOf())

    private val adapter by lazy {
        TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    internal inline val fragmentActivity
        get() = requireActivity() as MainActivity

    internal inline val application
        get() = requireActivity().application as MainApplication

    override val mutex = Mutex()

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    internal companion object {
        private const val COLLAPSED_HEIGHT = 250
        private const val UNINITIALIZED = -1
        private const val NOT_FOUND = -2

        @JvmStatic
        internal fun newInstance() = CurPlaylistTrackListFragment()
    }

    override fun getTheme() = R.style.AppBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentCurTrackListBinding>(
            inflater,
            R.layout.fragment_cur_track_list,
            container,
            false
        ).apply {
            viewModel = CurPlaylistTrackListViewModel(this@CurPlaylistTrackListFragment)

            updater = trackSwipeRefreshLayout.apply {
                setColorSchemeColors(Params.instance.primaryColor)
                setOnRefreshListener {
                    try {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIForPlayingTrackList(isLocking = true)
                            isRefreshing = false
                        }
                    } catch (ignored: Exception) {
                        // permissions not given
                    }
                }
            }

            this@CurPlaylistTrackListFragment.amountOfTracks = amountOfTracks

            try {
                runOnUIThread {
                    val task = loadAsync()

                    while (true) {
                        try {
                            awaitDialog = createAndShowAwaitDialog(requireContext(), false)
                            break
                        } catch (ignored: Exception) {
                        }
                    }

                    task.join()
                    awaitDialog?.dismiss()
                    adapter.setCurrentList(itemList)

                    amountOfTracks.isSelected = true
                    listeningLength.isSelected = true
                    setTrackAmountText(itemList)

                    recyclerView = trackRecyclerView.apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@CurPlaylistTrackListFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        isNestedScrollingEnabled = true

                        ItemTouchHelper(
                            object : ItemTouchHelper.SimpleCallback(
                                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                                ItemTouchHelper.START or ItemTouchHelper.END
                            ) {
                                override fun isLongPressDragEnabled() = true
                                override fun isItemViewSwipeEnabled() = true

                                @SuppressLint("SyntheticAccessor")
                                override fun onSelectedChanged(
                                    viewHolder: RecyclerView.ViewHolder?,
                                    actionState: Int
                                ) {
                                    super.onSelectedChanged(viewHolder, actionState)

                                    runOnUIThread {
                                        this@CurPlaylistTrackListFragment.adapter
                                            .setCurrentList(application.curPlaylist.enumerated())
                                    }

                                    updater!!.isEnabled =
                                        actionState != ItemTouchHelper.ACTION_STATE_DRAG
                                }

                                @SuppressLint("SyntheticAccessor")
                                override fun onMove(
                                    recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder
                                ) = this@CurPlaylistTrackListFragment.adapter.onMove(
                                    viewHolder.bindingAdapterPosition,
                                    target.bindingAdapterPosition
                                )

                                @SuppressLint("SyntheticAccessor")
                                override fun onSwiped(
                                    viewHolder: RecyclerView.ViewHolder,
                                    direction: Int
                                ) = this@CurPlaylistTrackListFragment.adapter.onRemove(
                                    viewHolder.bindingAdapterPosition
                                )
                            }
                        ).attachToRecyclerView(this)
                    }
                }
            } catch (ignored: Exception) {
                // permissions not given
            }

            amountOfTracks.isSelected = true
            listeningLength.isSelected = true
            setTrackAmountText(itemList)
        }

        setListeningLength()
        return binding!!.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacker = context as Callbacks?
    }

    override fun onDetach() {
        callbacker = null
        super.onDetach()
    }

    /**
     * Initializes [BottomSheetBehavior] for the fragment.
     * It's allowed to scroll down (only), move tracks and update UI
     */

    override fun onStart() {
        super.onStart()
        val density = requireContext().resources.displayMetrics.density

        dialog?.let {
            val bottomSheet = it.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as FrameLayout

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.peekHeight = (COLLAPSED_HEIGHT * density).toInt()
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    /** Cleans UI and removes reference to the fragment for [MainActivity] */
    override fun onStop() {
        super.onStop()
        updater!!.clearAnimation()
        updater!!.clearDisappearingChildren()
        updater!!.clearFocus()
        updater!!.isEnabled = false
        fragmentActivity.curPlaylistFragment = WeakReference(null)
    }

    /**
     * Sets both UI and [MainActivity.curPlaylistFragment]
     * reference to the fragment for [MainActivity]
     */

    override fun onResume() {
        super.onResume()
        updater!!.isEnabled = true
        beforeFragment = fragmentActivity.currentFragment
        fragmentActivity.curPlaylistFragment = WeakReference(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
        awaitDialog?.dismiss()
        awaitDialog = null
        binding = null
        recyclerView = null
        updater = null
        amountOfTracks = null
    }

    /**
     * Clears all tracks and sets [MainActivity.currentFragment]
     * reference to the [beforeFragment]
     */

    override fun onDestroy() {
        super.onDestroy()
        itemList.clear()
        fragmentActivity.currentFragment = beforeFragment
    }

    override val loadedContent get() = itemList

    /** Gets current playlist */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                itemList.run {
                    clear()
                    addAll(application.curPlaylist.enumerated())
                }
            } catch (ignored: Exception) {
            }
        }
    }

    /** Updates UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: List<Pair<Int, Track>>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setTrackAmountText(src)
        setListeningLength()
    }

    /** Like [UIUpdatable.updateUIAsync] but src is [itemList] */
    override suspend fun updateUIForPlayingTrackList(isLocking: Boolean) =
        updateUIAsync(itemList, isLocking)

    /**
     * Loads content with [loadAsync]
     * and updates UI with [updateUIAsync]
     */

    override fun updateUIOnChangeContentForPlayingTrackListAsync() = runOnUIThread {
        val task = loadAsync()
        awaitDialog = createAndShowAwaitDialog(requireContext(), false)

        task.join()
        awaitDialog?.dismiss()
        updateUIForPlayingTrackList(isLocking = true)
    }

    /** Shuffles all tracks */
    override fun onShuffleButtonPressedForPlayingTrackListAsync() = runOnUIThread {
        this@CurPlaylistTrackListFragment.updateUIAsync(itemList.shuffled(), isLocking = true)
    }

    override suspend fun loadForPlayingTrackListAsync() = loadAsync()
    override suspend fun highlightAsync(path: String) = adapter.highlightAsync(path)

    private fun setTrackAmountText(src: List<*>) {
        amountOfTracks!!.text = "${resources.getString(R.string.tracks)}: ${src.size}"
    }

    /** Sets duration to listen the whole playlist */
    private fun setListeningLength() {
        application
            .curPlaylist
            .fold(BigInteger("0")) { acc, track -> acc + BigInteger("${track.duration}") }
            .let { it / BigInteger("60000") }
            .toLong()
            .toFormattedTimeString()
            .let { binding!!.listeningLength.text = "${resources.getString(R.string.duration)}: $it" }
    }

    /** [RecyclerView.Adapter] for [CurPlaylistTrackListFragment] */

    inner class TrackAdapter : AsyncListDifferAdapter<Pair<Int, Track>, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(
            first: Pair<Int, Track>,
            second: Pair<Int, Track>
        ) = first.first == second.first && first.second == second.second

        /** [RecyclerView.ViewHolder] for tracks of [TrackAdapter] */

        inner class TrackHolder(internal val trackBinding: ListItemTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            internal lateinit var track: Track
                private set

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                callbacker?.onTrackSelected(track, differ.currentList.tracks)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            internal fun bind(_track: Track) {
                trackBinding.tracks = differ.currentList.tracks.toTypedArray()
                trackBinding.viewModel = TrackItemViewModel(_pos = layoutPosition + 1, _track)
                trackBinding.executePendingBindings()
                track = _track

                if (Params.instance.isCoversDisplayed) runOnUIThread {
                    try {
                        val albumImage = trackBinding.trackAlbumImage
                        val task = application.getAlbumPictureAsync(track.path)

                        Glide.with(this@CurPlaylistTrackListFragment)
                            .load(task.await())
                            .placeholder(R.drawable.album_default)
                            .skipMemoryCache(true)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .override(albumImage.width, albumImage.height)
                            .into(albumImage)
                    } catch (ignored: Exception) {
                        // Image is to big to show
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackHolder(
            ListItemTrackBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: TrackHolder, position: Int) = holder.run {
            bind(differ.currentList[position].second)
            trackBinding.trackItemSettings.setOnClickListener {
                fragmentActivity.onTrackSettingsButtonClicked(
                    it,
                    differ.currentList[position].second,
                    BottomSheetBehavior.STATE_EXPANDED
                )
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param path path of track to highlight
         */

        internal fun highlightAsync(path: String) = runOnWorkerThread {
            application.run {
                val oldPath = highlightedPath
                var oldInd = oldPath?.let { UNINITIALIZED } ?: NOT_FOUND
                var newInd = UNINITIALIZED
                var ind = 0

                while (ind < currentList.size && (oldInd == UNINITIALIZED || newInd == UNINITIALIZED)) {
                    val curItem = currentList[ind]
                    if (oldInd != NOT_FOUND && curItem.second.path == oldPath) oldInd = ind
                    if (curItem.second.path == path) newInd = ind
                    ind++
                }

                launch(Dispatchers.Main) {
                    oldInd.takeIf { it != NOT_FOUND }?.let(this@TrackAdapter::notifyItemChanged)
                    notifyItemChanged(newInd)
                }.join()
            }
        }

        /** Removes track and updates UI */
        @SuppressLint("SyntheticAccessor")
        internal fun onRemove(ind: Int) {
            runOnWorkerThread {
                when {
                    fragmentActivity.removeTrackFromQueue(
                        application.curPlaylist[ind],
                        willUpdateUI = false
                    ) -> launch(Dispatchers.Main) {
                        notifyItemRemoved(ind)
                        setTrackAmountText(application.curPlaylist)
                        setListeningLength()

                        recyclerView!!.itemAnimator = null
                        adapter.setCurrentList(application.curPlaylist.enumerated())
                        delay(1000) // waiting for adapter to update
                        recyclerView!!.itemAnimator = DefaultItemAnimator()
                    }

                    else -> launch(Dispatchers.Main) { notifyItemChanged(ind) }
                }
            }
        }

        /** On track's position changed */
        internal fun onMove(fromInd: Int, toInd: Int): Boolean {
            when {
                fromInd < toInd -> (fromInd until toInd).forEach {
                    Collections.swap(application.curPlaylist, it, it + 1)
                }

                else -> (fromInd downTo toInd + 1).forEach {
                    Collections.swap(application.curPlaylist, it, it - 1)
                }
            }

            runOnIOThread {
                StorageUtil
                    .getInstanceAsyncSynchronized()
                    .storeCurPlaylistLocking(application.curPlaylist)
            }

            notifyItemMoved(fromInd, toInd) // TODO: not working
            return true
        }
    }
}