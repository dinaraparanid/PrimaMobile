package com.dinaraparanid.prima.utils.polymorphism

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Some
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ancestor for all tracks fragments
 */

abstract class AbstractTrackListFragment<B : ViewDataBinding> :
    TrackListSearchFragment<AbstractTrack,
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

    public override var adapter: TrackAdapter? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        if (fragmentActivity.isUpdateNeeded) {
            updateUIOnChangeTracks()
            fragmentActivity.isUpdateNeeded = false
        }

        adapter?.highlight(application.curPath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    override fun updateUI(src: List<AbstractTrack>) {
        try {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                adapter = TrackAdapter(src).apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                recyclerView!!.adapter = adapter
                setEmptyTextViewVisibility(src)

                val text = "${resources.getString(R.string.tracks)}: ${src.size}"
                amountOfTracks!!.text = text
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onQueryTextChange(query: String?): Boolean {
        super.onQueryTextChange(query)
        val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
        amountOfTracks!!.text = txt
        return true
    }

    fun updateUIOnChangeTracks() {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            val task = loadAsync()
            val progress = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            progress.dismiss()
            updateUI()
        }
    }

    /**
     * [RecyclerView.Adapter] for [TypicalTrackListFragment]
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<AbstractTrack>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

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
                (callbacker as Callbacks?)?.onTrackSelected(track, tracks)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: AbstractTrack) {
                trackBinding.viewModel = TrackItemViewModel(layoutPosition + 1)
                trackBinding.tracks = tracks.toTypedArray()
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

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit = holder.run {
            bind(tracks[position])
            trackBinding.trackItemSettings.setOnClickListener {
                fragmentActivity.trackSettingsButtonAction(
                    it,
                    tracks[position],
                    BottomSheetBehavior.STATE_COLLAPSED
                )
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param path path of track to highlight
         */

        @Synchronized
        @SuppressLint("NotifyDataSetChanged")
        fun highlight(path: String): Unit = application.run {
            highlightedRow = Some(path)
            notifyDataSetChanged()
        }
    }
}