package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Ancestor for all tracks fragments */

abstract class AbstractTrackListFragment<B : ViewDataBinding> : TrackListSearchFragment<
        AbstractTrack,
        AbstractTrackListFragment<B>.TrackAdapter,
        AbstractTrackListFragment<B>.TrackAdapter.TrackHolder, B>(),
    PlayingTrackList<AbstractTrack> {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Plays track or just shows playing bar
         * @param track track to show in playing bar
         * @param tracks tracks from which current playlist' ll be constructed
         * @param needToPlay if true track' ll be played
         * else it' ll be just shown in playing bar
         */

        fun onTrackSelected(
            track: AbstractTrack,
            tracks: Collection<AbstractTrack>,
            needToPlay: Boolean = true
        )
    }

    private companion object {
        private const val UNINITIALIZED = -1
        private const val NOT_FOUND = -2
    }

    override var _adapter: TrackAdapter? = null

    override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!)
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
    }

    final override suspend fun updateUIAsyncNoLock(src: List<Pair<Int, AbstractTrack>>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)

        val text = "${resources.getString(R.string.tracks)}: ${src.size}"
        amountOfTracks!!.text = text
    }

    final override fun onQueryTextChange(query: String?): Boolean {
        runOnUIThread {
            filterAsync(query)?.join()
            val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
            amountOfTracks!!.text = txt
        }

        return true
    }

    override fun onDestroyView() {
        Glide.get(requireContext()).clearMemory()
        super.onDestroyView()
    }

    final override fun initAdapter() {
        _adapter = TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    final override fun onShuffleButtonPressedForPlayingTrackListAsync() = onShuffleButtonPressed()

    final override suspend fun updateUIForPlayingTrackList(isLocking: Boolean) =
        updateUIAsync(isLocking)

    final override fun updateUIOnChangeContentForPlayingTrackListAsync() =
        updateUIOnChangeContentAsync()

    final override suspend fun loadForPlayingTrackListAsync() = loadAsync()

    final override suspend fun highlightAsync(path: String) = _adapter?.highlightAsync(path)

    /** [RecyclerView.Adapter] for [TypicalViewTrackListFragment] */

    inner class TrackAdapter : AsyncListDifferAdapter<Pair<Int, AbstractTrack>, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(
            first: Pair<Int, AbstractTrack>,
            second: Pair<Int, AbstractTrack>
        ) = first.first == second.first && first.second == second.second

        /** [RecyclerView.ViewHolder] for tracks of [TrackAdapter] */

        inner class TrackHolder(internal val trackBinding: ListItemTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: AbstractTrack

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(
                    track, differ.currentList.tracks
                )
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            internal fun bind(_track: AbstractTrack) {
                trackBinding.tracks = differ.currentList.tracks.toTypedArray()
                trackBinding.viewModel = TrackItemViewModel(_pos = layoutPosition + 1, _track)
                trackBinding.executePendingBindings()
                track = _track

                if (Params.instance.areCoversDisplayed)
                    runOnUIThread {
                        try {
                            val albumImage = trackBinding.trackAlbumImage
                            val task = application.getAlbumPictureAsync(track.path)

                            Glide.with(this@AbstractTrackListFragment)
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
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
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
                    BottomSheetBehavior.STATE_COLLAPSED
                )
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param path path of track to highlight
         */

        internal fun highlightAsync(path: String) = runOnWorkerThread {
            application.run {
                val oldPath = highlightedPath.orNull()
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
                }
            }
        }
    }
}