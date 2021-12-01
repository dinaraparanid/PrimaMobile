package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Some
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Ancestor for all tracks fragments
 */

abstract class AbstractTrackListFragment<B : ViewDataBinding> : TrackListSearchFragment<
        AbstractTrack,
        AbstractTrackListFragment<B>.TrackAdapter,
        AbstractTrackListFragment<B>.TrackAdapter.TrackHolder, B>() {
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

    public override val adapter by lazy {
        TrackAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault

        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    final override suspend fun updateUIAsync(src: List<AbstractTrack>) = coroutineScope {
        launch(Dispatchers.Main) {
            try {
                adapter.setCurrentList(src)
                recyclerView!!.adapter = adapter
                setEmptyTextViewVisibility(src)

                val text = "${resources.getString(R.string.tracks)}: ${src.size}"
                amountOfTracks!!.text = text
            } catch (ignored: Exception) {
            }
        }
    }

    final override fun onQueryTextChange(query: String?): Boolean {
        super.onQueryTextChange(query)
        val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
        amountOfTracks!!.text = txt
        return true
    }

    /** [RecyclerView.Adapter] for [TypicalTrackListFragment] */

    inner class TrackAdapter : AsyncListDifferAdapter<AbstractTrack, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(first: AbstractTrack, second: AbstractTrack) = first == second
        override val self: AsyncListDifferAdapter<AbstractTrack, TrackHolder> get() = this

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(internal val trackBinding: ListItemTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: AbstractTrack

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(track, differ.currentList)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: AbstractTrack) {
                trackBinding.viewModel = TrackItemViewModel(layoutPosition + 1)
                trackBinding.tracks = differ.currentList.toTypedArray()
                trackBinding.track = _track
                trackBinding.executePendingBindings()
                track = _track
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

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit = holder.run {
            bind(differ.currentList[position])
            trackBinding.trackItemSettings.setOnClickListener {
                fragmentActivity.onTrackSettingsButtonClicked(
                    it,
                    differ.currentList[position],
                    BottomSheetBehavior.STATE_COLLAPSED
                )
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param path path of track to highlight
         */

        internal suspend fun highlight(path: String) = coroutineScope {
            launch(Dispatchers.Default) {
                application.run {
                    val oldPath = highlightedRow.orNull()?.toCharArray()?.joinToString("")
                    highlightedRow = Some(path)

                    var oldInd = oldPath?.let { UNINITIALIZED } ?: NOT_FOUND
                    var newInd = UNINITIALIZED
                    var ind = 0

                    while (ind < currentList.size && (oldInd == UNINITIALIZED || newInd == UNINITIALIZED)) {
                        val curItem = currentList[ind]
                        if (oldInd != NOT_FOUND && curItem.path == oldPath) oldInd = ind
                        if (curItem.path == path) newInd = ind
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
}