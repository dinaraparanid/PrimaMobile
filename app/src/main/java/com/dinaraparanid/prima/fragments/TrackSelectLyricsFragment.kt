package com.dinaraparanid.prima.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentTrackLyricsFoundBinding
import com.dinaraparanid.prima.databinding.ListItemGeniusTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.GetHappiApiKeyDialog
import com.dinaraparanid.prima.utils.polymorphism.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import com.dinaraparanid.prima.utils.web.genius.search_response.DataOfData
import com.dinaraparanid.prima.viewmodels.androidx.TrackListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import kotlinx.coroutines.*

/**
 * Shows all found tracks with lyrics.
 * Requires internet connection
 */

class TrackSelectLyricsFragment :
    TrackListSearchFragment<GeniusTrack,
            TrackSelectLyricsFragment.TrackAdapter,
            TrackSelectLyricsFragment.TrackAdapter.TrackHolder,
            FragmentTrackLyricsFoundBinding>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Shows fragment with lyrics of selected track
         * @param track track which lyrics should be displayed
         */

        fun onTrackSelected(track: GeniusTrack)
    }

    @Deprecated("Should not be used")
    override lateinit var amountOfTracks: carbon.widget.TextView

    @Deprecated("Should not be used")
    override lateinit var trackOrderTitle: carbon.widget.TextView

    override lateinit var emptyTextView: TextView

    private lateinit var title: String
    private lateinit var artist: String

    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentTrackLyricsFoundBinding? = null
    override var adapter: TrackAdapter? = TrackAdapter(listOf())

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    private val geniusFetcher: GeniusFetcher by lazy { GeniusFetcher() }

    internal companion object {
        private const val TITLE_KEY = "title"
        private const val ARTIST_KEY = "artist"
        private const val ITEM_LIST_KEY = "item_list"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param title track's title to search
         * @param artist track's artist to search
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            title: String,
            artist: String,
        ) = TrackSelectLyricsFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(TITLE_KEY, title)
                putString(ARTIST_KEY, artist)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        title = requireArguments().getString(TITLE_KEY)!!
        artist = requireArguments().getString(ARTIST_KEY)!!
        mainLabelCurText = resources.getString(R.string.lyrics)

        val load = {
            itemListSearch.addAll(itemList)
            adapter = TrackAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            try {
                setEmptyTextViewVisibility(itemList)
            } catch (ignored: Exception) {
                // not initialized
            }
        }

        savedInstanceState?.getSerializable(ITEM_LIST_KEY)?.let {
            itemList.addAll(it as Array<GeniusTrack>)
            load()
        } ?: run {
            viewModel.viewModelScope.launch {
                loadAsync().join()
                load()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentTrackLyricsFoundBinding>(
                inflater,
                R.layout.fragment_track_lyrics_found,
                container,
                false
            )
            .apply {
                binding = this
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()

                updater = trackLyricsFoundSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        this@TrackSelectLyricsFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                            loadAsync().join()
                            updateUI()
                            isRefreshing = false
                        }
                    }
                }

                emptyTextView = trackLyricsEmpty
                setEmptyTextViewVisibility(itemList)

                recyclerView = trackLyricsFoundRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = this@TrackSelectLyricsFragment.adapter?.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }
                    addItemDecoration(VerticalSpaceItemDecoration(30))
                    addItemDecoration(DividerItemDecoration(requireActivity()))
                }
            }

        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_change_api, menu)
        (menu.findItem(R.id.lyrics_find).actionView as SearchView).setOnQueryTextListener(this)
        menu.findItem(R.id.lyrics_find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.lyrics_change_api) {
            GetHappiApiKeyDialog {
                StorageUtil(requireContext()).storeHappiApiKey(it)
                (requireActivity() as MainActivity).showSelectLyricsFragment(it)
            }.show(requireActivity().supportFragmentManager, null)

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://happi.dev/panel?ref=home")
                )
            )
        }

        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(ITEM_LIST_KEY, itemList.toTypedArray())
        super.onSaveInstanceState(outState)
    }

    override fun updateUI(src: List<GeniusTrack>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = TrackAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.Main) {
            if (itemList.isEmpty())
                geniusFetcher
                    .fetchTrackDataSearch("$artist $title")
                    .observe(viewLifecycleOwner) {
                        itemList.clear()

                        when (it.meta.status) {
                            in 200 until 300 -> Toast.makeText(
                                requireContext(),
                                R.string.genius_query_error,
                                Toast.LENGTH_LONG
                            ).show()

                            else -> itemList.addAll(it.response.hits.map(DataOfData::result))
                        }

                        updateUI()
                    }
        }
    }

    /**
     * [RecyclerView.Adapter] for [TrackSelectLyricsFragment]
     * @param tracks tracks to use in adapter
     */


    inner class TrackAdapter(private val tracks: List<GeniusTrack>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(private val trackBinding: ListItemGeniusTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: GeniusTrack

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(track)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: GeniusTrack) {
                trackBinding.track = _track
                trackBinding.viewModel = TrackItemViewModel(layoutPosition + 1)
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

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(tracks[position])
    }
}