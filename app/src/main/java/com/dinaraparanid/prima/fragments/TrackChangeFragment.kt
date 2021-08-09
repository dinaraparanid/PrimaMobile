package com.dinaraparanid.prima.fragments

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import carbon.widget.ImageView
import carbon.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.web.FoundTrack
import com.dinaraparanid.prima.utils.web.HappiFetcher
import com.dinaraparanid.prima.viewmodels.TrackChangeViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Fragment to change track's metadata.
 * @since Android 11 it only changes entities in app,
 * but not metadata of track itself
 */

class TrackChangeFragment : CallbacksFragment(), Rising {
    internal interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Makes selected image new track's album image
         * @param image image to select
         * @param replaceTrack track which metadata is replacing
         */
        fun onImageSelected(image: Bitmap, replaceTrack: Track)

        /**
         * Makes changeable track's metadata
         * equal to selected found track's metadata
         * @param selectedTrack track to select
         * @param replaceTrack track which metadata is replacing
         */

        fun onTrackSelected(selectedTrack: FoundTrack, replaceTrack: Track)
    }

    private lateinit var track: Track
    private lateinit var apiKey: String
    private lateinit var titleInput: EditText
    private lateinit var artistInput: EditText
    private lateinit var albumInput: EditText
    private lateinit var curImage: ImageView
    private lateinit var imagesEmpty: carbon.widget.TextView
    private lateinit var tracksEmpty: carbon.widget.TextView
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var tracksRecyclerView: RecyclerView

    private var imagesAdapter: ImageAdapter? = null
    private var tracksAdapter: TrackAdapter? = null

    private val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    internal companion object {
        private const val TRACK_KEY = "track"
        private const val API_KEY = "api_key"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param track track to change
         * @param apiKey api key to use internet features
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            track: Track,
            apiKey: String
        ) = TrackChangeFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TRACK_KEY, track)
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putString(API_KEY, apiKey)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        track = requireArguments().getSerializable(TRACK_KEY) as Track
        apiKey = requireArguments().getString(API_KEY)!!
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_track_info, container, false)

        curImage = view.findViewById(R.id.current_image)

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            Glide.with(this@TrackChangeFragment)
                .load(
                    (requireActivity().application as MainApplication)
                        .getAlbumPictureAsync(track.path, true)
                        .await()
                )
                .into(curImage)
        }

        val tableLayout: TableLayout = view.findViewById(R.id.track_change_table_layout)

        val titleRow = tableLayout.findViewById<TableRow>(R.id.title_change_row).apply {
            findViewById<TextView>(R.id.track_title_change).run {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
        }

        titleInput = titleRow.findViewById<EditText>(R.id.track_title_change_input).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setHintTextColor(Color.GRAY)
            setText(track.title, TextView.BufferType.EDITABLE)
        }

        val artistRow = tableLayout.findViewById<TableRow>(R.id.artist_change_row).apply {
            findViewById<TextView>(R.id.track_artist_change).run {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
        }

        artistInput = artistRow.findViewById<EditText>(R.id.track_artist_change_input).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setHintTextColor(Color.GRAY)
            setText(track.artist, TextView.BufferType.EDITABLE)
        }

        val albumRow = tableLayout.findViewById<TableRow>(R.id.album_change_row).apply {
            findViewById<TextView>(R.id.track_album_change).run {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
        }

        albumInput = albumRow.findViewById<EditText>(R.id.track_album_change_input).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setHintTextColor(Color.GRAY)
            setText(track.playlist, TextView.BufferType.EDITABLE)
        }

        view.findViewById<carbon.widget.TextView>(R.id.track_metadata_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        view.findViewById<carbon.widget.TextView>(R.id.empty_images).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        view.findViewById<carbon.widget.TextView>(R.id.album_images_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        view.findViewById<carbon.widget.TextView>(R.id.empty_similar_tracks).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        view.findViewById<carbon.widget.TextView>(R.id.similar_tracks_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        HappiFetcher()
            .fetchTrackDataSearch("${track.artist} ${track.title}", apiKey)
            .observe(viewLifecycleOwner) {
                GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create()
                    .fromJson(it, HappiFetcher.ParseObject::class.java)
                    .run {
                        val trackList = mutableListOf<FoundTrack>().apply {
                            addAll(
                                when {
                                    this@run != null && success -> result

                                    else -> {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.wrong_api_key,
                                            Toast.LENGTH_LONG
                                        ).show()

                                        arrayOf()
                                    }
                                }
                            )
                        }

                        tracksAdapter = TrackAdapter(trackList).apply {
                            stateRestorationPolicy =
                                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }

                        imagesAdapter = ImageAdapter(trackList.map(FoundTrack::cover)).apply {
                            stateRestorationPolicy =
                                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }

                        tracksRecyclerView = view
                            .findViewById<RecyclerView>(R.id.similar_tracks_recycler_view)
                            .apply {
                                layoutManager = LinearLayoutManager(requireContext())
                                adapter = tracksAdapter
                                addItemDecoration(VerticalSpaceItemDecoration(30))
                            }

                        imagesRecyclerView = view
                            .findViewById<RecyclerView>(R.id.images_recycler_view)
                            .apply {
                                layoutManager = LinearLayoutManager(
                                    requireContext(),
                                    LinearLayoutManager.HORIZONTAL,
                                    false
                                )

                                adapter = imagesAdapter
                                addItemDecoration(HorizontalSpaceItemDecoration(30))
                            }

                        imagesEmpty =
                            view.findViewById<carbon.widget.TextView>(R.id.empty_images).apply {
                                visibility = when {
                                    trackList.isEmpty() -> carbon.widget.TextView.VISIBLE
                                    else -> carbon.widget.TextView.INVISIBLE
                                }

                                typeface = (requireActivity().application as MainApplication)
                                    .getFontFromName(Params.instance.font)
                            }

                        tracksEmpty =
                            view.findViewById<carbon.widget.TextView>(R.id.empty_similar_tracks)
                                .apply {
                                    visibility = when {
                                        trackList.isEmpty() -> carbon.widget.TextView.VISIBLE
                                        else -> carbon.widget.TextView.INVISIBLE
                                    }

                                    typeface = (requireActivity().application as MainApplication)
                                        .getFontFromName(Params.instance.font)
                                }
                    }

                if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
            }

        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_accept, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.accept) {
            val act = requireActivity() as MainActivity

            viewModel.viewModelScope.launch {
                launch(Dispatchers.IO) {
                    val track = Track(
                        track.androidId,
                        titleInput.text.toString(),
                        artistInput.text.toString(),
                        albumInput.text.toString(),
                        track.path,
                        track.duration,
                        track.relativePath,
                        track.displayName,
                        track.addDate
                    )

                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            (act.application as MainApplication)
                                .changedTracks[track.path] = track
                        }

                        else -> {
                            val content = ContentValues().apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                                put(MediaStore.Audio.Media.TITLE, titleInput.text.toString())
                                put(MediaStore.Audio.Media.ARTIST, artistInput.text.toString())
                                put(MediaStore.Audio.Media.ALBUM, albumInput.text.toString())
                            }

                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                val uri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    track.androidId
                                )

                                act.contentResolver.update(
                                    uri, ContentValues().apply {
                                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                                    }, null, null
                                )

                                act.contentResolver.update(
                                    ContentUris.withAppendedId(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        track.androidId
                                    ),
                                    content,
                                    "${MediaStore.Audio.Media.RELATIVE_PATH} = ?" +
                                            " AND ${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                                    arrayOf(track.relativePath, track.displayName)
                                )
                            } else act.contentResolver.update(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                content,
                                "${MediaStore.Audio.Media.DATA} = ?",
                                arrayOf(track.path)
                            )
                        }
                    }

                    runBlocking {
                        launch(Dispatchers.Main) {
                            if ((act.application as MainApplication).curPath == track.path)
                                act.updateUI(track to false)
                        }
                    }

                    (act.application as MainApplication).curPlaylist.replace(
                        this@TrackChangeFragment.track,
                        track
                    )
                }

                launch(Dispatchers.IO) {
                    CustomPlaylistsRepository.instance
                        .getTrackAsync(track.path).await()
                        ?.let { (androidId,
                                    id,
                                    _, _, _,
                                    playlistId,
                                    path,
                                    duration,
                                    relativePath,
                                    displayName,
                                    addDate) ->
                            CustomPlaylistsRepository.instance.updateTrackAsync(
                                CustomPlaylistTrack(
                                    androidId,
                                    id,
                                    titleInput.text.toString(),
                                    artistInput.text.toString(),
                                    albumInput.text.toString(),
                                    playlistId,
                                    path,
                                    duration,
                                    relativePath,
                                    displayName,
                                    addDate
                                )
                            )
                        }
                }


                launch(Dispatchers.IO) {
                    FavouriteRepository.instance.updateTrackAsync(FavouriteTrack(track))
                }
            }

            act.supportFragmentManager.popBackStack()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun up() {
        if (!(requireActivity() as MainActivity).upped)
            tracksRecyclerView.layoutParams =
                (tracksRecyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = (requireActivity() as MainActivity).playingToolbarHeight
                }
    }

    /**
     * [androidx.recyclerview.widget.RecyclerView.Adapter]
     * for [TrackChangeFragment] (tracks)
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<FoundTrack>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: FoundTrack

            private val titleTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_found_lyrics_title)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val artistsAlbumTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_found_lyrics_author_album)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            private val trackNumberTextView: TextView = itemView
                .findViewById<TextView>(R.id.track_found_lyrics_number)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(track, this@TrackChangeFragment.track)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: FoundTrack) {
                track = _track

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${track.playlist}"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                layoutInflater.inflate(
                    R.layout.list_item_track_lyrics_found,
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(tracks[position])
    }

    /**
     * [androidx.recyclerview.widget.RecyclerView.Adapter]
     * for [TrackChangeFragment] (images)
     * @param images links to images
     */


    inner class ImageAdapter(private val images: List<String>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<ImageAdapter.ImageHolder>() {

        /**
         * [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class ImageHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var image: Bitmap

            private val imageView = itemView.findViewById<ImageView>(R.id.image_item).apply {
                if (!Params.instance.isRoundingPlaylistImage) setCornerRadius(0F)
            }

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onImageSelected(image, track)
            }

            /**
             * Constructs GUI for image item
             * @param _image track to bind and use
             */

            fun bind(_image: String) {
                Glide.with(this@TrackChangeFragment)
                    .load(_image)
                    .placeholder(R.drawable.album_default)
                    .skipMemoryCache(true)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .override(imageView.width, imageView.height)
                    .into(imageView)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder =
            ImageHolder(layoutInflater.inflate(R.layout.list_item_image, parent, false))

        override fun getItemCount(): Int = images.size

        override fun onBindViewHolder(holder: ImageHolder, position: Int): Unit =
            holder.bind(images[position])
    }
}