package com.dinaraparanid.prima.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.viewmodels.TrackListViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

class TrackListFragment : Fragment(), SearchView.OnQueryTextListener {
    interface Callbacks {
        fun onTrackSelected(track: Track, needToPlay: Boolean = true)
    }

    private lateinit var trackRecyclerView: RecyclerView
    internal var adapter: TrackAdapter? = TrackAdapter(mutableListOf())
    private var callbacks: Callbacks? = null
    private val playlist = Playlist()
    private var mainLabelOldText = "Tracks"
    private var isMain = true
    internal val trackListViewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    companion object {
        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            playlist: Playlist,
            isMain: Boolean = true,
            _firstToHighlight: String? = null
        ): TrackListFragment = TrackListFragment().apply {
            arguments = Bundle().apply {
                putSerializable("playlist", playlist)
                putString("main_label_old_text", mainLabelOldText)
                putBoolean("is_main", isMain)
                putString("start", _firstToHighlight ?: "______ЫЫЫЫЫЫЫЫ______")
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
        playlist.addAll((arguments?.getSerializable("playlist") as Playlist?) ?: listOf())

        trackListViewModel.run {
            load(
                savedInstanceState?.getStringArrayList("highlight_rows"),
                savedInstanceState?.getBoolean("highlighted_start")
            )

            Thread.sleep(100)

            if (!highlightedStartLiveData.value!!)
                arguments?.getString("start")
                    ?.takeIf { it != "______ЫЫЫЫЫЫЫЫ______" }
                    ?.let {
                        highlightRowsLiveData.value!!.add(it)
                        highlightRowsLiveData.value =
                            highlightRowsLiveData.value!!.distinct().toMutableList()
                        highlightedStartLiveData.value = true
                    }
        }
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
            isMain -> "Tracks"
            else -> "Current Playlist"
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(playlist.toList())
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelOldText
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(
            "highlight_rows",
            ArrayList(trackListViewModel.highlightRowsLiveData.value!!)
        )

        outState.putBoolean(
            "highlighted_start",
            trackListViewModel.highlightedStartLiveData.value!!
        )

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            playlist.toList(),
            query ?: ""
        )

        adapter?.run {
            replaceAll(filteredModelList)
            notifyDataSetChanged()
        }

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

    internal inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        internal inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: Track
            val titleTextView: TextView = itemView.findViewById(R.id.track_title)
            val artistsAlbumTextView: TextView =
                itemView.findViewById(R.id.track_author_album)
            private val trackNumberTextView: TextView = itemView.findViewById(R.id.track_number)

            init {
                itemView.setOnClickListener(this)
                itemView
                    .findViewById<TextView>(R.id.track_title)
                    .setTextColor(ViewSetter.textColor)
                itemView
                    .findViewById<TextView>(R.id.track_author_album)
                    .setTextColor(ViewSetter.textColor)
            }

            override fun onClick(v: View?) {
                callbacks?.onTrackSelected(track)
            }

            fun bind(_track: Track) {
                track = _track
                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") "Unknown artist" else it }
                    } / ${
                        track.album
                            .let {
                                if (it == "<unknown>" || it == track
                                        .path
                                        .split('/')
                                        .takeLast(2)
                                        .first()
                                ) "Unknown album" else it
                            }
                    }"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") "Unknown track" else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

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
            holder.bind(
                when {
                    isMain -> trackList[position]
                    else -> playlist.toList()[position]
                }
            )

            val trackTitle = holder.titleTextView
            val trackAlbumArtist = holder.artistsAlbumTextView

            when (trackList[position].path) {
                in trackListViewModel.highlightRowsLiveData.value!! -> {
                    val color = Params.getInstance().theme.rgb
                    trackTitle.setTextColor(color)
                    trackAlbumArtist.setTextColor(color)

                    arguments?.getString("start")?.let {
                        trackListViewModel.highlightRowsLiveData.value!!.remove(it)
                    }
                }

                else -> {
                    val color = ViewSetter.textColor
                    trackTitle.setTextColor(color)
                    trackAlbumArtist.setTextColor(color)
                }
            }
        }

        fun replaceAll(models: Collection<Track>) = trackList.run {
            beginBatchedUpdates()
            (size() - 1 downTo 0).forEach {
                get(it).let { track -> if (track !in models) remove(track) }
            }

            addAll(models)
            endBatchedUpdates()
        }

        fun highlight(track: Track) {
            trackListViewModel.highlightRowsLiveData.value!!.clear()
            trackListViewModel.highlightRowsLiveData.value!!.add(tracks.find { it.id == track.id }!!.path)
            trackListViewModel.highlightRowsLiveData.value =
                trackListViewModel.highlightRowsLiveData.value!!.distinct().toMutableList()
            notifyDataSetChanged()
        }
    }
}