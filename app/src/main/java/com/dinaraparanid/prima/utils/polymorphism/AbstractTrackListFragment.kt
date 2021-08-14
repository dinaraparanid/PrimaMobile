package com.dinaraparanid.prima.utils.polymorphism

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import carbon.widget.ImageView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.viewmodels.androidx.TrackListViewModel
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

    override lateinit var emptyTextView: TextView
    protected lateinit var trackAmountImage: carbon.widget.TextView
    protected lateinit var trackOrderButton: ImageView
    protected lateinit var trackOrderTitle: carbon.widget.TextView

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
                trackAmountImage.text = text
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onQueryTextChange(query: String?): Boolean {
        super.onQueryTextChange(query)
        val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
        trackAmountImage.text = txt
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

    protected fun updateOrderTitle(): Unit = trackOrderTitle.run {
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

    /**
     * [RecyclerView.Adapter] for [TypicalTrackListFragment]
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: Track

            val titleTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_title)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            val settingsButton: ImageView = itemView.findViewById(R.id.track_item_settings)

            val artistsAlbumTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_author_album)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val trackNumberTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_number)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

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
                track = _track

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${
                        NativeLibrary.playlistTitle(
                            track.playlist.toByteArray(),
                            track.path.toByteArray(),
                            resources.getString(R.string.unknown_album).toByteArray()
                        )
                    }"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            holder.bind(tracks[position])

            val trackTitle = holder.titleTextView
            val trackAlbumArtist = holder.artistsAlbumTextView
            val settingsButton = holder.settingsButton
            val highlightedRows = (requireActivity().application as MainApplication).highlightedRows

            settingsButton.setOnClickListener {
                (requireActivity() as MainActivity)
                    .trackSettingsButtonAction(
                        it,
                        tracks[position],
                        BottomSheetBehavior.STATE_COLLAPSED
                    )
            }

            val color = when (tracks[position].path) {
                in highlightedRows -> Params.instance.theme.rgb
                else -> Params.instance.fontColor
            }

            trackTitle.setTextColor(color)
            trackAlbumArtist.setTextColor(color)
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