package com.app.musicplayer.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.app.musicplayer.MainActivity
import com.app.musicplayer.R
import com.app.musicplayer.viewmodels.TrackListViewModel
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.MusicRepository
import com.app.musicplayer.utils.Params
import com.app.musicplayer.utils.VerticalSpaceItemDecoration

class TrackListFragment private constructor() : Fragment(), SearchView.OnQueryTextListener {
    interface Callbacks {
        fun onTrackSelected(track: Track, ret: Boolean = false)
    }

    private lateinit var trackRecyclerView: RecyclerView
    private var adapter: TrackAdapter? = TrackAdapter(mutableListOf())
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
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

        (requireActivity() as AppCompatActivity).let { act ->
            act.supportActionBar?.run {
                setDisplayHomeAsUpEnabled(true)
                setHomeAsUpIndicator(
                    BitmapDrawable(
                        resources,
                        Bitmap.createScaledBitmap(
                            (resources.getDrawable(
                                R.drawable.burger_white,
                                act.theme
                            ) as BitmapDrawable).bitmap,
                            30,
                            30,
                            true
                        )
                    )
                )
                title = ""
            }
        }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_track_list, menu)
        (menu.findItem(R.id.find_track).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(trackListViewModel.trackListLiveData.value, query ?: "")
        adapter?.replaceAll(filteredModelList)
        trackRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun updateUI(tracks: List<Track>) {
        adapter = TrackAdapter(tracks.toMutableList())
        trackRecyclerView.adapter = adapter
    }

    private fun filter(models: Collection<Track>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter {
                lowerCase in it.title.lowercase() ||
                        MusicRepository
                            .getInstance()
                            .getArtistsByTrack(it.trackId)
                            ?.any { (artist) -> lowerCase in artist.name } == true
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
                    item1.trackId == item2.trackId
            }
        ).apply { addAll(tracks) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))

        override fun getItemCount() = trackList.size()

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            (activity!! as MainActivity).run {
                tracks.apply { for (i in 0 until trackList.size()) add(trackList[i]) }
                tracks = tracks.distinctBy { it.trackId }.toMutableList()
            }

            holder.bind(trackList[position])
        }

        fun replaceAll(models: Collection<Track>) = trackList.run {
            beginBatchedUpdates()
            (size() - 1 downTo 0).forEach {
                get(it).let { track ->
                    if (track !in models)
                        remove(track)
                }
            }

            addAll(models)
            endBatchedUpdates()
        }
    }
}