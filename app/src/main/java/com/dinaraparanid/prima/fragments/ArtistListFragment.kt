package com.dinaraparanid.prima.fragments

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.FilterFragment
import com.dinaraparanid.prima.utils.polymorphism.RecyclerViewUp
import com.dinaraparanid.prima.utils.polymorphism.UIUpdatable
import java.util.Locale

class ArtistListFragment :
    Fragment(),
    SearchView.OnQueryTextListener,
    UIUpdatable<List<Artist>>,
    FilterFragment<Artist>,
    RecyclerViewUp {
    interface Callbacks {
        fun onArtistSelected(artist: Artist, playlist: Playlist)
    }

    private lateinit var artistRecyclerView: RecyclerView
    private lateinit var mainLabelOldText: String
    private lateinit var mainLabelCurText: String
    private lateinit var titleDefault: String

    private var adapter: ArtistAdapter? = ArtistAdapter(mutableListOf())
    private var callbacks: Callbacks? = null
    private val artists = mutableListOf<Artist>()
    private val artistsSearch = mutableListOf<Artist>()
    private val playlist = Playlist()
    private var tracksLoaded = false

    companion object {
        private const val ARTISTS_KEY = "artists"
        private const val MAIN_LABEL_OLD_TEXT_KEY = "main_label_old_text"
        private const val MAIN_LABEL_CUR_TEXT_KEY = "main_label_cur_text"

        @JvmStatic
        fun newInstance(
            artists: Array<Artist>,
            mainLabelOldText: String,
            mainLabelCurText: String
        ): ArtistListFragment = ArtistListFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARTISTS_KEY, artists)
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
        artists.addAll(requireArguments().getSerializable(ARTISTS_KEY) as Array<Artist>)
        artistsSearch.addAll(artists)
        adapter = ArtistAdapter(artistsSearch)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_artists, container, false)
        titleDefault = resources.getString(R.string.artists)

        artistRecyclerView = view.findViewById<RecyclerView>(R.id.artists_recycler_view).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ArtistListFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(30))
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateUI(artistsSearch)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onStop() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelOldText
        super.onStop()
    }

    override fun onResume() {
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            artists,
            query ?: ""
        )

        artistsSearch.clear()
        artistsSearch.addAll(filteredModelList)
        adapter!!.notifyDataSetChanged()
        updateUI(artistsSearch)

        artistRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun updateUI(src: List<Artist>) {
        adapter = ArtistAdapter(src)
        artistRecyclerView.adapter = adapter
    }

    override fun filter(models: Collection<Artist>?, query: String): List<Artist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.name.lowercase() } ?: listOf()
        }

    internal fun loadTracks(artist: Artist) {
        val selection = "${MediaStore.Audio.Media.ARTIST} = ?"
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
            arrayOf(artist.name),
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

    override fun up() {
        artistRecyclerView.layoutParams =
            (artistRecyclerView.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = 200
            }
    }

    private inner class ArtistAdapter(private val artists: List<Artist>) :
        RecyclerView.Adapter<ArtistAdapter.ArtistHolder>() {
        private inner class ArtistHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var artist: Artist

            private val artistNameTextView = itemView
                .findViewById<TextView>(R.id.artist_name)
                .apply { setTextColor(ViewSetter.textColor) }

            private val artistImage: TextView = itemView.findViewById(R.id.artist_image)
            val settingsButton: ImageButton = itemView.findViewById(R.id.artist_item_settings)

            init {
                itemView.setOnClickListener(this)
                settingsButton.setImageResource(ViewSetter.settingsButtonImage)
            }

            override fun onClick(v: View?) {
                loadTracks(artist)
                while (!tracksLoaded) Unit
                tracksLoaded = false
                callbacks?.onArtistSelected(artist, playlist)
            }

            fun bind(_artist: Artist) {
                artist = _artist
                artistNameTextView.text = artist.name

                artistImage.run {
                    text = artist.name.trim().let { name ->
                        when (name) {
                            resources.getString(R.string.unknown_artist) -> "?"
                            else -> name.split(" ").take(2).map { s ->
                                s.replaceFirstChar {
                                    when {
                                        it.isLowerCase() -> it.titlecase(Locale.getDefault())
                                        else -> it.toString()
                                    }
                                }.first()
                            }.fold("") { acc, c -> "$acc$c" }
                        }
                    }

                    setTextColor(Params.getInstance().theme.rgb)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ArtistHolder(layoutInflater.inflate(R.layout.list_item_artist, parent, false))

        override fun getItemCount() = artists.size

        override fun onBindViewHolder(holder: ArtistHolder, position: Int) = holder.run {
            val artist = artists[position]
            bind(artist)

            settingsButton.setOnClickListener {
                (requireActivity() as MainActivity)
                    .artistSettingsButtonAction(it, artist)
            }
        }
    }
}