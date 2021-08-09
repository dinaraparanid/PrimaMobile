package com.dinaraparanid.prima.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.GetHappiApiKeyDialog
import com.dinaraparanid.prima.utils.polymorphism.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment
import com.dinaraparanid.prima.utils.web.FoundTrack
import com.dinaraparanid.prima.utils.web.HappiFetcher
import com.dinaraparanid.prima.viewmodels.TrackListViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*

/**
 * Shows all found tracks with lyrics.
 * Requires internet connection
 */

class TrackSelectLyricsFragment :
    TrackListSearchFragment<FoundTrack, TrackSelectLyricsFragment.TrackAdapter.TrackHolder>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Shows fragment with lyrics of selected track
         * @param track track which lyrics should be displayed
         */

        fun onTrackSelected(track: FoundTrack)
    }

    override lateinit var emptyTextView: TextView
    override lateinit var updater: SwipeRefreshLayout

    private lateinit var track: Track
    private lateinit var apiKey: String

    override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? =
        TrackAdapter(mutableListOf())

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    internal companion object {
        private const val TRACK_KEY = "track"
        private const val API_KEY = "api_key"
        private const val ITEM_LIST_KEY = "item_list"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param track track to search
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            track: Track,
            apiKey: String
        ) = TrackSelectLyricsFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putSerializable(TRACK_KEY, track)
                putString(API_KEY, apiKey)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        track = requireArguments().getSerializable(TRACK_KEY)!! as Track
        apiKey = requireArguments().getString(API_KEY)!!
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
            itemList.addAll(it as Array<FoundTrack>)
            load()
        } ?: run {
            viewModel.viewModelScope.launch {
                loadAsync().await()
                load()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_lyrics_found, container, false)

        updater = view
            .findViewById<SwipeRefreshLayout>(R.id.track_lyrics_found_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.instance.theme.rgb)
                setOnRefreshListener {
                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        loadAsync().await()
                        updateUI()
                        isRefreshing = false
                    }
                }
            }

        val constraintLayout: ConstraintLayout =
            updater.findViewById(R.id.track_lyrics_found_constraint_layout)

        emptyTextView = constraintLayout.findViewById<TextView>(R.id.track_lyrics_empty).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }
        setEmptyTextViewVisibility(itemList)

        recyclerView = constraintLayout
            .findViewById<RecyclerView>(R.id.track_lyrics_found_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TrackSelectLyricsFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }
                addItemDecoration(VerticalSpaceItemDecoration(30))
                addItemDecoration(DividerItemDecoration(requireActivity()))
            }

        return view
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

    override fun updateUI(src: List<FoundTrack>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = TrackAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.Main) {
            if (itemList.isEmpty())
                HappiFetcher()
                    .fetchTrackDataSearchWithLyrics("${track.artist} ${track.title}", apiKey)
                    .observe(viewLifecycleOwner) {
                        itemList.clear()

                        GsonBuilder()
                            .excludeFieldsWithoutExposeAnnotation()
                            .create()
                            .fromJson(it, HappiFetcher.ParseObject::class.java)
                            .run {
                                when {
                                    this != null && success -> result.let(itemList::addAll)

                                    else -> Toast.makeText(
                                        requireContext(),
                                        R.string.wrong_api_key,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                        updateUI()
                    }
        }
    }

    /**
     * [RecyclerView.Adapter] for [TrackSelectLyricsFragment]
     * @param tracks tracks to use in adapter
     */


    inner class TrackAdapter(private val tracks: List<FoundTrack>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: FoundTrack

            private val titleTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_found_lyrics_title)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val artistsAlbumTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_found_lyrics_author_album)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val trackNumberTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_found_lyrics_number)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

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

            fun bind(_track: FoundTrack) {
                track = _track

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${track.playlist}"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                layoutInflater.inflate(
                    R.layout.list_item_track_lyrics_found,
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(tracks[position])
    }
}