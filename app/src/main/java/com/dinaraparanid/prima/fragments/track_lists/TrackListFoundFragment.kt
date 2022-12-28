package com.dinaraparanid.prima.fragments.track_lists

import android.os.Build
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
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentTracksFoundBinding
import com.dinaraparanid.prima.databinding.ListItemGeniusTrackBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.mvvmp.view.fragments.TrackListSearchFragment
import com.dinaraparanid.prima.mvvmp.view.fragments.setMainLabelInitializedSync
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import com.dinaraparanid.prima.utils.web.genius.search_response.DataOfData
import com.dinaraparanid.prima.mvvmp.androidx.TrackListFoundViewModel
import com.dinaraparanid.prima.mvvmp.old_shit.TrackItemViewModel
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*

/**
 * Shows all found tracks.
 * Requires internet connection
 */

class TrackListFoundFragment :
    TrackListSearchFragment<GeniusTrack,
            TrackListFoundFragment.TrackAdapter,
            TrackListFoundFragment.TrackAdapter.TrackHolder,
            FragmentTracksFoundBinding>() {
    interface Callbacks : CallbacksFragment.CallbackHandler {
        /**
         * Shows fragment with lyrics of selected track
         * @param track track which lyrics should be displayed
         */

        suspend fun onTrackSelected(track: GeniusTrack, target: Target)
    }

    enum class Target { LYRICS, INFO }

    @Deprecated("Should not be used")
    override var amountOfTracks: carbon.widget.TextView? = null

    @Deprecated("Should not be used")
    override var trackOrderTitle: carbon.widget.TextView? = null

    override var emptyTextView: TextView? = null

    private lateinit var title: String
    private lateinit var artist: String
    private lateinit var target: Target
    private var awaitDialog: Deferred<KProgressHUD>? = null

    @Volatile
    private var isLoaded = false
    private val loadingCondition = AsyncCondVar()

    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentTracksFoundBinding? = null
    override var _adapter: TrackAdapter? = null

    override val viewModel: TrackListFoundViewModel by lazy {
        ViewModelProvider(this)[TrackListFoundViewModel::class.java]
    }

    private val geniusFetcher by lazy { GeniusFetcher() }

    companion object {
        private const val TITLE_KEY = "title"
        private const val ARTIST_KEY = "artist"
        private const val ITEM_LIST_KEY = "item_list"
        private const val TARGET_KEY = "target"

        /**
         * Creates new instance of fragment with params
         * @param title track's title to search
         * @param artist track's artist to search
         * @param target reason of why this fragment is created
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            title: String,
            artist: String,
            target: Target
        ) = TrackListFoundFragment().apply {
            arguments = Bundle().apply {
                putString(TITLE_KEY, title)
                putString(ARTIST_KEY, artist)
                putInt(TARGET_KEY, target.ordinal)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelText.set(resources.getString(R.string.tracks))
        title = requireArguments().getString(TITLE_KEY)!!
        artist = requireArguments().getString(ARTIST_KEY)!!
        target = Target.values()[requireArguments().getInt(TARGET_KEY)]

        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentTracksFoundBinding>(
                inflater,
                R.layout.fragment_tracks_found,
                container,
                false
            )
            .apply {
                viewModel = BasePresenter()

                updater = trackLyricsFoundSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }

                emptyTextView = trackLyricsEmpty
                setEmptyTextViewVisibility(itemList)

                recyclerView = trackLyricsFoundRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    initAdapter()
                    adapter = this@TrackListFoundFragment.adapter
                    addItemDecoration(VerticalSpaceItemDecoration(30))

                    runOnUIThread {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
                            Params.getInstanceSynchronized().areDividersShown
                        ) addItemDecoration(
                            DividerItemDecoration(requireContext(), Divider.instance)
                        )
                    }
                }
            }

        fun load() {
            itemListSearch.addAll(itemList)
            initAdapter()
            runOnUIThread { adapter.setCurrentList(itemList) }
            setEmptyTextViewVisibility(itemList)
        }

        (savedInstanceState?.getSerializable(ITEM_LIST_KEY) as Array<Pair<Int, GeniusTrack>>?)
            ?.unzip()
            ?.second
            ?.toTypedArray()
            .also(viewModel::load)
            ?.also {
                itemList.addAll(it.enumerated())
                load()
            } ?: run {
            runOnIOThread {
                awaitDialog = async(Dispatchers.Main) {
                    createAndShowAwaitDialog(requireContext(), false)
                }

                loadAsync().join()

                while (!isLoaded)
                    loadingCondition.blockAsync()

                launch(Dispatchers.Main) {
                    load()
                    updateUIAsync(isLocking = true)
                    awaitDialog?.await()?.dismiss()
                }
            }
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_select_lyrics, menu)
        (menu.findItem(R.id.lyrics_find).actionView as SearchView)
            .setOnQueryTextListener(this@TrackListFoundFragment)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.lyrics_find_by) selectSearch()
        return super.onMenuItemSelected(menuItem)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(ITEM_LIST_KEY, itemList.toTypedArray())
        super.onSaveInstanceState(outState)
    }

    /** Refreshes UI */
    override fun onResume() {
        super.onResume()
        runOnUIThread {
            updateUIAsync(
                src = viewModel.trackListFlow.value.enumerated(),
                isLocking = true
            )
        }
    }

    /** Clears dialogs */
    override fun onDestroyView() {
        super.onDestroyView()
        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    /** Updates UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: List<Pair<Int, GeniusTrack>>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /** Loads all tracks from the get request's response */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.Main) {
            if (itemList.isEmpty())
                geniusFetcher
                    .fetchTrackDataSearch("$artist $title")
                    .observe(viewLifecycleOwner) { response ->
                        itemList.clear()

                        when (response.meta.status) {
                            !in 200 until 300 -> {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.no_internet_connection,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> response.response.hits.map(DataOfData::result).let {
                                itemList.addAll(it.enumerated())
                                viewModel.trackListFlow.value = it.toMutableList()
                            }
                        }

                        if (!isLoaded) {
                            isLoaded = true
                            runOnUIThread { loadingCondition.openAsync() }
                        }
                    }
        }
    }

    /** Initializes adapter */
    override fun initAdapter() {
        _adapter = TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [TrackListFoundFragment] */

    inner class TrackAdapter : AsyncListDifferAdapter<Pair<Int, GeniusTrack>, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(first: Pair<Int, GeniusTrack>, second: Pair<Int, GeniusTrack>) =
            first.first == second.first && first.second == second.second

        /** [RecyclerView.ViewHolder] for tracks of [TrackAdapter] */

        inner class TrackHolder(private val trackBinding: ListItemGeniusTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: GeniusTrack

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                runOnWorkerThread {
                    (callbackHandler as Callbacks?)?.onTrackSelected(track, target)
                }
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: GeniusTrack) {
                trackBinding.track = _track
                trackBinding.viewModel = TrackItemViewModel(_pos = layoutPosition + 1)
                trackBinding.executePendingBindings()
                track = _track
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                ListItemGeniusTrackBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
            )

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(differ.currentList[position].second)
    }
}