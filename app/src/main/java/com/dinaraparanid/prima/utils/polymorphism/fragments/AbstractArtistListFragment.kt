package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.databinding.FragmentArtistsBinding
import com.dinaraparanid.prima.databinding.ListItemArtistBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.kaopiz.kprogresshud.KProgressHUD

/** Ancestor [ListFragment] for all artist list fragments */

abstract class AbstractArtistListFragment : MainActivityUpdatingListFragment<
        Artist,
        AbstractArtistListFragment.ArtistAdapter,
        AbstractArtistListFragment.ArtistAdapter.ArtistHolder,
        FragmentArtistsBinding>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Creates new [TypicalViewTrackListFragment] with artist's tracks
         * @param artist artist himself
         */

        fun onArtistSelected(artist: Artist)
    }

    final override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    final override var _adapter: ArtistAdapter? = null

    final override var emptyTextView: TextView? = null
    final override var updater: SwipeRefreshLayout? = null
    final override var binding: FragmentArtistsBinding? = null
    private var awaitDialog: KProgressHUD? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentArtistsBinding>(inflater, R.layout.fragment_artists, container, false)
            .apply {
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ArtistListViewModel()
                emptyTextView = artistsEmpty

                updater = artistSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }
            }

        runOnUIThread {
            val task = loadAsync()
            awaitDialog = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            awaitDialog?.dismiss()
            initAdapter()

            itemListSearch.addAll(itemList)
            adapter.setCurrentList(itemList)
            setEmptyTextViewVisibility(itemList)

            recyclerView = binding!!.artistsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@AbstractArtistListFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

            if (application.playingBarIsVisible) up()
        }

        return binding!!.root
    }

    final override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    final override suspend fun updateUIAsyncNoLock(src: List<Artist>) {
        adapter.setCurrentList(src)
        setEmptyTextViewVisibility(src)
    }

    final override fun filter(models: Collection<Artist>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.name.lowercase() } ?: listOf()
        }

    final override fun initAdapter() {
        _adapter = ArtistAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [AbstractArtistListFragment] */
    inner class ArtistAdapter : AsyncListDifferAdapter<Artist, ArtistAdapter.ArtistHolder>() {
        override fun areItemsEqual(first: Artist, second: Artist) = first == second

        /** [RecyclerView.ViewHolder] for artists of [ArtistAdapter] */
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

            internal fun bind(_artist: Artist) {
                artistBinding.viewModel = binding!!.viewModel!!
                artistBinding.artist = _artist
                artistBinding.artistItemSettings.imageTintList = ViewSetter.colorStateList
                artistBinding.executePendingBindings()
                artist = _artist
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ArtistHolder(
            ListItemArtistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: ArtistHolder, position: Int): Unit = holder.run {
            runOnUIThread {
                val artist = differ.currentList[position]
                bind(artist)

                holder.artistBinding.artistItemSettings.setOnClickListener {
                    fragmentActivity.artistSettingsButtonAction(it, artist)
                }
            }
        }
    }
}