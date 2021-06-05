package com.dinaraparanid.prima.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.viewmodels.TrackListViewModel
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration

class TrackListFragment : Fragment(), SearchView.OnQueryTextListener {
    interface Callbacks {
        fun onTrackSelected(track: Track)
    }

    private lateinit var trackRecyclerView: RecyclerView
    private var adapter: TrackAdapter? = TrackAdapter(mutableListOf())
    private var callbacks: Callbacks? = null
    internal val trackListViewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    companion object {
        @JvmStatic
        fun newInstance(
            mainLabelOldText: String,
            playlist: Playlist,
            isMain: Boolean = true
        ): TrackListFragment = TrackListFragment().apply {
            arguments = Bundle().apply {
                putSerializable("playlist", playlist)
                putString("main_label_old_text", mainLabelOldText)
                putBoolean("is_main", isMain)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        trackListViewModel.load(
            arguments?.getSerializable("playlist") as Playlist?,
            arguments?.getString("main_label_old_text"),
            arguments?.getBoolean("is_main")
        )
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

        (requireActivity() as MainActivity).mainLabel.text = when {
            trackListViewModel.isMain.value!! -> "Tracks"
            else -> "Current Playlist"
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(trackListViewModel.playlist.value!!.toList())
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainLabel.text =
            trackListViewModel.mainLabelOldText.value!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(
            "main_label_old_text",
            (requireActivity() as MainActivity).mainLabel.text.toString()
        )

        outState.putSerializable(
            "playlist",
            trackListViewModel.playlist.value
        )

        outState.putSerializable(
            "is_main",
            trackListViewModel.isMain.value
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            trackListViewModel.playlist.value!!.toList(),
            query ?: ""
        )

        adapter?.replaceAll(filteredModelList)
        trackRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    private fun updateUI(tracks: List<Track>) {
        adapter = TrackAdapter(tracks.toMutableList())
        trackRecyclerView.adapter = adapter
    }

    private fun filter(models: Collection<Track>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter {
                lowerCase in it.title.lowercase() || lowerCase in it.artist.lowercase()
            } ?: listOf()
        }

    private inner class TrackHolder(view: View) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var track: Track
        private val titleTextView: TextView = itemView.findViewById(R.id.track_title)
        private val artistsAlbumTextView: TextView = itemView.findViewById(R.id.track_author_album)

        init {
            itemView.setOnClickListener(this)
            itemView
                .findViewById<TextView>(R.id.track_title)
                .setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
            itemView
                .findViewById<TextView>(R.id.track_author_album)
                .setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        }

        fun bind(_track: Track) {
            track = _track
            val artistAlbum = "${track.artist} / ${track.album}"

            titleTextView.text = track.title
            artistsAlbumTextView.text = artistAlbum
        }

        override fun onClick(v: View?) {
            callbacks?.onTrackSelected(track)
        }
    }

    private inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackHolder>() {
        val trackList = SortedList(
            Track::class.java,
            object : SortedList.Callback<Track>() {
                override fun compare(o1: Track, o2: Track) = o1.title.compareTo(o2.title)

                override fun onInserted(position: Int, count: Int) =
                    notifyItemRangeInserted(position, count)

                override fun onRemoved(position: Int, count: Int) =
                    notifyItemRangeRemoved(position, count)

                override fun onMoved(fromPosition: Int, toPosition: Int) =
                    notifyItemMoved(fromPosition, toPosition)

                override fun onChanged(position: Int, count: Int) =
                    notifyItemRangeChanged(position, count)

                override fun areContentsTheSame(oldItem: Track, newItem: Track) = oldItem == newItem

                override fun areItemsTheSame(item1: Track, item2: Track) =
                    item1.id == item2.id
            }
        ).apply { addAll(tracks) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))

        override fun getItemCount() = trackList.size()

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            (activity!! as MainActivity).run {
                mainActivityViewModel.tracks.apply {
                    (0 until trackList.size()).forEach { add(trackList[it]) }
                }
                mainActivityViewModel.tracks = tracks.distinctBy { it.id }.toMutableList()
            }

            holder.bind(
                when {
                    trackListViewModel.isMain.value!! -> trackList[position]
                    else -> trackListViewModel.playlist.value!!.toList()[position]
                }
            )
        }

        fun replaceAll(models: Collection<Track>) = trackList.run {
            beginBatchedUpdates()
            (size() - 1 downTo 0).forEach {
                get(it).let { track -> if (track !in models) remove(track) }
            }

            addAll(models)
            endBatchedUpdates()
        }
    }
}