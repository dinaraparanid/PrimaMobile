package com.dinaraparanid.prima.utils.polymorphism

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import carbon.widget.TextView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.viewmodels.androidx.TrackListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ancestor for all tracks fragments
 */

abstract class AbstractTrackListFragment :
    TrackListSearchFragment<Track, AbstractTrackListFragment.TrackAdapter.TrackHolder>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Plays track or just shows playing bar
         * @param track track to show in playing bar
         * @param tracks tracks from which current playlist' ll be constructed
         * @param needToPlay if true track' ll be played
         * else it' ll be just shown in playing bar
         */

        fun onTrackSelected(
            track: Track,
            tracks: Collection<Track>,
            needToPlay: Boolean = true
        )
    }

    public override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? = null

    override val viewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    protected abstract var amountOfTracks: TextView
    protected abstract var trackOrderTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

    override fun onResume() {
        super.onResume()

        val act = requireActivity() as MainActivity

        if (act.needToUpdate) {
            updateUIOnChangeTracks()
            act.needToUpdate = false
        }
    }

    override fun updateUI(src: List<Track>) {
        try {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                adapter = TrackAdapter(src).apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }
                recyclerView.adapter = adapter
                setEmptyTextViewVisibility(src)

                val text = "${resources.getString(R.string.tracks)}: ${src.size}"
                amountOfTracks.text = text
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onQueryTextChange(query: String?): Boolean {
        super.onQueryTextChange(query)
        val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
        amountOfTracks.text = txt
        return true
    }

    fun updateUIOnChangeTracks() {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().join()
            updateUI()
        }
    }

    /**
     * Updates title of tracks ordering
     */

    internal fun updateOrderTitle(): Unit = trackOrderTitle.run {
        val txt = "${
            resources.getString(
                when (Params.instance.tracksOrder.first) {
                    Params.Companion.TracksOrder.TITLE -> R.string.by_title
                    Params.Companion.TracksOrder.ARTIST -> R.string.by_artist
                    Params.Companion.TracksOrder.ALBUM -> R.string.by_album
                    else -> R.string.by_date
                }
            )
        } ${
            when {
                Params.instance.tracksOrder.second -> "ᐯ"
                else -> "ᐱ"
            }
        }"

        text = txt
    }

    internal fun onTrackOrderButtonPressed(view: View) = PopupMenu(requireContext(), view).run {
        menuInflater.inflate(R.menu.menu_track_order, menu)

        val f = Params.instance.tracksOrder.first
        val s = Params.instance.tracksOrder.second

        menu.findItem(R.id.asc).isChecked = Params.instance.tracksOrder.second
        menu.findItem(R.id.desc).isChecked = !Params.instance.tracksOrder.second

        menu.findItem(R.id.order_title).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.TITLE

        menu.findItem(R.id.order_artist).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.ARTIST

        menu.findItem(R.id.order_album).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.ALBUM

        menu.findItem(R.id.order_date).isChecked =
            Params.instance.tracksOrder.first == Params.Companion.TracksOrder.DATE

        setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.asc -> Params.instance.tracksOrder = f to true
                R.id.desc -> Params.instance.tracksOrder = f to false

                R.id.order_title -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.TITLE to s

                R.id.order_artist -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.ARTIST to s

                R.id.order_album -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.ALBUM to s

                else -> Params.instance.tracksOrder =
                    Params.Companion.TracksOrder.DATE to s
            }

            updateOrderTitle()
            StorageUtil(requireContext()).storeTrackOrder(Params.instance.tracksOrder)
            updateUI(Params.sortedTrackList(itemList))
            true
        }

        show()
    }

    internal fun onShuffleButtonPressed() = updateUI(itemList.shuffled())

    /**
     * [RecyclerView.Adapter] for [TypicalTrackListFragment]
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(internal val trackBinding: ListItemTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: Track

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

            fun bind(_track: Track) {
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
                (requireActivity() as MainActivity)
                    .trackSettingsButtonAction(
                        it,
                        tracks[position],
                        BottomSheetBehavior.STATE_COLLAPSED
                    )
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param track track to highlight
         */

        @SuppressLint("NotifyDataSetChanged")
        fun highlight(track: Track): Unit =
            (requireActivity().application as MainApplication).run {
                highlightedRows.clear()
                highlightedRows.add(track.path)
                highlightedRows = highlightedRows.distinct().toMutableList()
                notifyDataSetChanged()
            }
    }
}