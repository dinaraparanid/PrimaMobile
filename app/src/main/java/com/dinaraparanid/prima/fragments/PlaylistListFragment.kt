package com.dinaraparanid.prima.fragments

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.MainApplication
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter

class PlaylistListFragment : Fragment(), SearchView.OnQueryTextListener {
    interface Callbacks {
        fun onPlaylistSelected(playlist: Playlist)
    }

    private lateinit var playlistRecyclerView: RecyclerView
    private lateinit var mainLabelOldText: String
    private lateinit var mainLabelCurText: String

    private var adapter: PlaylistAdapter? = null
    private var callbacks: Callbacks? = null
    private val playlists = mutableListOf<Playlist>()
    private val playlistsSearch = mutableListOf<Playlist>()
    private var tracksLoaded = false

    companion object {
        private const val PLAYLISTS_KEY = "playlists"
        private const val MAIN_LABEL_OLD_TEXT_KEY = "main_label_old_text"
        private const val MAIN_LABEL_CUR_TEXT_KEY = "main_label_cur_text"
        private const val TITLE_DEFAULT = "Playlists"

        @JvmStatic
        internal fun newInstance(
            playlists: Array<Playlist>,
            mainLabelOldText: String,
            mainLabelCurText: String
        ): PlaylistListFragment = PlaylistListFragment().apply {
            arguments = Bundle().apply {
                putSerializable(PLAYLISTS_KEY, Playlist.List(playlists))
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
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
        playlists.addAll((requireArguments().getSerializable(PLAYLISTS_KEY) as Playlist.List).playlists)
        playlistsSearch.addAll(playlists)
        adapter = PlaylistAdapter(playlistsSearch)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: TITLE_DEFAULT
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: TITLE_DEFAULT
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)

        playlistRecyclerView = view.findViewById<RecyclerView>(R.id.playlist_recycler_view).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@PlaylistListFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(30))
            addItemDecoration(HorizontalSpaceItemDecoration(30))
        }

        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(playlistsSearch.toList())
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelOldText
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            playlists,
            query ?: ""
        )

        playlistsSearch.clear()
        playlistsSearch.addAll(filteredModelList)
        adapter!!.notifyDataSetChanged()
        updateUI(playlistsSearch)

        playlistRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    private fun updateUI(playlists: List<Playlist>) {
        adapter = PlaylistAdapter(playlists)
        playlistRecyclerView.adapter = adapter
    }

    private inline fun filter(models: Collection<Playlist>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    internal fun loadTracks(playlist: Playlist) {
        val selection = "${MediaStore.Audio.Media.ALBUM} = ?"
        val order = MediaStore.Audio.Media.TITLE + " ASC"
        val trackList = mutableListOf<Track>()
        playlist.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        requireActivity().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(playlist.title),
            order
        ).use { cursor ->
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    trackList.add(
                        Track(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getLong(4),
                            cursor.getLong(5)
                        )
                    )
                }

                playlist.addAll(trackList.distinctBy { it.path })
            }
        }

        tracksLoaded = true
    }

    internal inner class PlaylistAdapter(private val playlists: List<Playlist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
        internal inner class PlaylistHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var playlist: Playlist

            private val titleTextView = itemView
                .findViewById<TextView>(R.id.playlist_title)
                .apply { setTextColor(ViewSetter.textColor) }

            private val playlistImage: ImageView = itemView
                .findViewById<CardView>(R.id.playlist_card_view)
                .findViewById(R.id.playlist_image)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                loadTracks(playlist)
                while (!tracksLoaded) Unit
                tracksLoaded = false
                callbacks?.onPlaylistSelected(playlist)
            }

            fun bind(_playlist: Playlist) {
                playlist = _playlist
                titleTextView.text = playlist.title
                playlistImage.setImageBitmap(
                    (requireActivity().application as MainApplication).getAlbumPicture(
                        playlist.currentTrack.path
                    )
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PlaylistHolder(layoutInflater.inflate(R.layout.list_item_playlist, parent, false))

        override fun getItemCount() = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) {
            holder.bind(playlists[position])
        }
    }
}