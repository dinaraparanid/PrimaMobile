package com.app.musicplayer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.MusicRepository
import com.app.musicplayer.utils.VerticalSpaceItemDecoration
import java.util.UUID

class TrackListFragment : Fragment() {
    interface Callbacks {
        fun onTrackSelected(trackId: UUID, isPlaying: Boolean = false)
    }

    private lateinit var trackRecyclerView: RecyclerView
    private var adapter: TrackAdapter? = TrackAdapter(listOf())
    private var callbacks: Callbacks? = null
    private val trackListViewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    companion object {
        fun newInstance() = TrackListFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_list, container, false)

        trackRecyclerView = view.findViewById(R.id.track_recycler_view)
        trackRecyclerView.layoutManager = LinearLayoutManager(context)
        trackRecyclerView.adapter = adapter
        trackRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(30))

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackListViewModel
            .trackListLiveData
            .observe(viewLifecycleOwner) { updateUI(it) }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateUI(tracks: List<Track>) {
        adapter = TrackAdapter(tracks)
        trackRecyclerView.adapter = adapter
    }

    private inner class TrackHolder(view: View) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var track: Track
        private val titleTextView: TextView = itemView.findViewById(R.id.track_title)
        private val artistsAlbumTextView: TextView = itemView.findViewById(R.id.track_author_album)

        init {
            itemView.setOnClickListener(this)
            itemView.setBackgroundColor(Params.getInstance().theme.rgb)
        }

        fun bind(_track: Track) {
            track = _track
            titleTextView.text = track.title
            artistsAlbumTextView.text = (MusicRepository
                .getInstance()
                .getArtistsByTrack(track.trackId)
                ?.map { it.artist.name }
                ?.fold("") { acc, x -> "$acc$x " } ?: "Unknown Artist"
            + " / ${
                track.albumId?.let {
                    MusicRepository
                        .getInstance()
                        .getAlbumOfTrack(it)
                } ?: "Unknown Album"
            }")
        }

        override fun onClick(v: View?) {
            callbacks?.onTrackSelected(track.trackId)
        }
    }

    private inner class TrackAdapter(val tracks: List<Track>) :
        RecyclerView.Adapter<TrackHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))

        override fun onBindViewHolder(holder: TrackHolder, position: Int) =
            holder.bind(tracks[position])

        override fun getItemCount() = tracks.size
    }
}