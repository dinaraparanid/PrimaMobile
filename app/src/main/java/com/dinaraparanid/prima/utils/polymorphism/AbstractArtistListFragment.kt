package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.databinding.FragmentArtistsBinding
import com.dinaraparanid.prima.databinding.ListItemArtistBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.androidx.ArtistListViewModel
import kotlinx.coroutines.*

/**
 * Ancestor [ListFragment] for all artist list fragments
 */

abstract class AbstractArtistListFragment :
    UpdatingListFragment<Artist, AbstractArtistListFragment.ArtistAdapter.ArtistHolder>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Creates new [TypicalTrackListFragment] with artist's tracks
         * @param artist artist himself
         */

        fun onArtistSelected(artist: Artist)
    }

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[ArtistListViewModel::class.java]
    }

    override var adapter: RecyclerView.Adapter<ArtistAdapter.ArtistHolder>? =
        ArtistAdapter(mutableListOf())

    override lateinit var emptyTextView: TextView
    override lateinit var updater: SwipeRefreshLayout

    private lateinit var binding: FragmentArtistsBinding
    private lateinit var artistViewModel: com.dinaraparanid.prima.viewmodels.mvvm.ArtistListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        titleDefault = resources.getString(R.string.artists)

        binding = DataBindingUtil
            .inflate<FragmentArtistsBinding>(inflater, R.layout.fragment_artists, container, false)
            .apply {
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ArtistListViewModel()
                artistViewModel = viewModel!!
            }

        emptyTextView = binding.artistsEmpty

        updater = binding.artistSwipeRefreshLayout.apply {
            setColorSchemeColors(Params.instance.primaryColor)
            setOnRefreshListener {
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    loadAsync().join()
                    updateUI()
                    isRefreshing = false
                }
            }
        }

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().join()
            itemListSearch.addAll(itemList)
            adapter = ArtistAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            setEmptyTextViewVisibility(itemList)

            recyclerView = binding.artistsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)

                adapter = this@AbstractArtistListFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

            if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        }

        (requireActivity() as MainActivity).binding.mainLabel.text = mainLabelCurText
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun updateUI(src: List<Artist>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = ArtistAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    override fun filter(models: Collection<Artist>?, query: String): List<Artist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.name.lowercase() } ?: listOf()
        }

    /**
     * [RecyclerView.Adapter] for [AbstractArtistListFragment]
     * @param artists artists to bind and use in adapter
     */

    inner class ArtistAdapter(private val artists: List<Artist>) :
        RecyclerView.Adapter<ArtistAdapter.ArtistHolder>() {
        /**
         * [RecyclerView.ViewHolder] for artists of [ArtistAdapter]
         */

        inner class ArtistHolder(internal val artistBinding: ListItemArtistBinding) :
            RecyclerView.ViewHolder(artistBinding.root),
            View.OnClickListener {
            private lateinit var artist: Artist

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onArtistSelected(artist)
            }

            /**
             * Constructs GUI for artist item
             * @param _artist artist to bind
             */

            fun bind(_artist: Artist) {
                artistBinding.viewModel = artistViewModel
                artistBinding.artist = _artist
                artistBinding.artistItemSettings.imageTintList = ViewSetter.colorStateList
                artistBinding.executePendingBindings()
                artist = _artist
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder =
            ArtistHolder(
                ListItemArtistBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = artists.size

        override fun onBindViewHolder(holder: ArtistHolder, position: Int): Unit = holder.run {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                val artist = artists[position]
                bind(artist)

                holder.artistBinding.artistItemSettings.setOnClickListener {
                    (requireActivity() as MainActivity)
                        .artistSettingsButtonAction(it, artist)
                }
            }
        }
    }
}