package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ImageView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.viewmodels.ArtistListViewModel
import kotlinx.coroutines.*

/**
 * Ancestor [ListFragment] for all artist list fragments
 */

abstract class AbstractArtistListFragment :
    ListFragment<Artist, AbstractArtistListFragment.ArtistAdapter.ArtistHolder>() {
    interface Callbacks : ListFragment.Callbacks {
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
        val view = inflater.inflate(R.layout.fragment_artists, container, false)
        titleDefault = resources.getString(R.string.artists)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.artist_swipe_refresh_layout)
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

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().await()
            itemListSearch.addAll(itemList)
            adapter = ArtistAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            val constraintLayout: carbon.widget.ConstraintLayout =
                updater.findViewById(R.id.artist_constraint_layout)

            emptyTextView = constraintLayout.findViewById<TextView>(R.id.artists_empty).apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
            setEmptyTextViewVisibility(itemList)

            recyclerView = constraintLayout
                .findViewById<carbon.widget.RecyclerView>(R.id.artists_recycler_view)
                .apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = this@AbstractArtistListFragment.adapter?.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }
                    addItemDecoration(VerticalSpaceItemDecoration(30))
                }

            if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        }

        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
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

        inner class ArtistHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var artist: Artist

            private val artistNameTextView = itemView
                .findViewById<TextView>(R.id.artist_name)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val artistImage: carbon.widget.TextView = itemView
                .findViewById<carbon.widget.TextView>(R.id.artist_image)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            val settingsButton: ImageView = itemView.findViewById(R.id.artist_item_settings)

            init {
                itemView.setOnClickListener(this)
                settingsButton.imageTintList = ViewSetter.colorStateList
            }

            override fun onClick(v: View?) {
                (callbacks as Callbacks?)?.onArtistSelected(artist)
            }

            /**
             * Constructs GUI for artist item
             * @param _artist artist to bind
             */

            fun bind(_artist: Artist) {
                artist = _artist
                artistNameTextView.text = artist.name

                artistImage.run {
                    text = artist.name.trim().let { name ->
                        when (name) {
                            resources.getString(R.string.unknown_artist) -> "?"
                            else -> NativeLibrary.artistImageBind(name.toByteArray())
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder =
            ArtistHolder(layoutInflater.inflate(R.layout.list_item_artist, parent, false))

        override fun getItemCount(): Int = artists.size

        override fun onBindViewHolder(holder: ArtistHolder, position: Int): Unit = holder.run {
            val artist = artists[position]
            bind(artist)

            settingsButton.setOnClickListener {
                (requireActivity() as MainActivity)
                    .artistSettingsButtonAction(it, artist)
            }
        }
    }
}