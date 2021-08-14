package com.dinaraparanid.prima.fragments

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentChangeTrackInfoBinding
import com.dinaraparanid.prima.databinding.ListItemImageBinding
import com.dinaraparanid.prima.databinding.ListItemTrackWithoutSettingsBinding
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.web.FoundTrack
import com.dinaraparanid.prima.utils.web.HappiFetcher
import com.dinaraparanid.prima.viewmodels.androidx.TrackChangeViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ChangeTrackViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*

/**
 * Fragment to change track's metadata.
 * @since Android 11 it only changes entities in app,
 * but not metadata of track itself
 */

class TrackChangeFragment :
    CallbacksFragment(),
    Rising,
    UIUpdatable<Pair<String, String>>,
    ChangeImageFragment {
    internal interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Makes selected image new track's album image
         * @param image image to select
         * @param albumImage album image view which image should be replaced
         */

        fun onImageSelected(image: Bitmap, albumImage: ImageView)

        /**
         * Makes changeable track's metadata
         * equal to selected found track's metadata
         * @param selectedTrack track to select
         * @param titleInput [EditText] for title
         * @param artistInput [EditText] for artist
         * @param albumInput [EditText] for album image
         */

        fun onTrackSelected(
            selectedTrack: FoundTrack,
            titleInput: EditText,
            artistInput: EditText,
            albumInput: EditText
        )
    }

    private lateinit var track: Track
    private lateinit var apiKey: String
    private lateinit var binding: FragmentChangeTrackInfoBinding
    private lateinit var mvvmViewModel: ChangeTrackViewModel

    private var imagesAdapter: ImageAdapter? = null
    private var tracksAdapter: TrackAdapter? = null

    internal val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    internal companion object {
        private const val ALBUM_IMAGE_PATH_KEY = "album_image_path"
        private const val ALBUM_IMAGE_URI_KEY = "album_image_uri"
        private const val TRACK_LIST_KEY = "track_list"
        private const val WAS_LOADED_KEY = "was_loaded"
        private const val TRACK_KEY = "track"
        private const val API_KEY = "api_key"
        private const val ADD_IMAGE_FROM_STORAGE = "add_image_from_storage"

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
        binding = DataBindingUtil.inflate<FragmentChangeTrackInfoBinding>(
            inflater,
            R.layout.fragment_change_track_info,
            container,
            false
        ).apply {
            viewModel = ChangeTrackViewModel()
            mvvmViewModel = viewModel
        }

        viewModel.load(
            savedInstanceState?.getBoolean(WAS_LOADED_KEY),
            savedInstanceState?.getString(ALBUM_IMAGE_PATH_KEY),
            savedInstanceState?.getParcelable(ALBUM_IMAGE_URI_KEY) as Uri?,
            savedInstanceState?.getSerializable(TRACK_LIST_KEY) as Array<FoundTrack>?
        )

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            viewModel.albumImagePathLiveData.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .load(it)
                    .into(binding.currentImage)
            } ?: viewModel.albumImageUriLiveData.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .load(it)
                    .into(binding.currentImage)
            } ?: Glide.with(this@TrackChangeFragment)
                .load(
                    (requireActivity().application as MainApplication)
                        .getAlbumPictureAsync(track.path, true)
                        .await()
                )
                .into(binding.currentImage)
        }

        binding.run {
            trackTitleChangeInput.setHintTextColor(Color.GRAY)
            trackArtistChangeInput.setHintTextColor(Color.GRAY)
            trackAlbumChangeInput.setHintTextColor(Color.GRAY)

            similarTracksRecyclerView.run {
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

            imagesRecyclerView.run {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                imagesAdapter = ImageAdapter(listOf(ADD_IMAGE_FROM_STORAGE)).apply {
                    stateRestorationPolicy =
                        androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                adapter = imagesAdapter

                addItemDecoration(HorizontalSpaceItemDecoration(30))
            }
        }

        when {
            viewModel.wasLoadedLiveData.value!! -> initRecyclerViews()
            else -> {
                viewModel.wasLoadedLiveData.value = true
                updateUI(track.artist to track.title)
            }
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).activityBinding.mainLabel.text = mainLabelCurText
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(WAS_LOADED_KEY, viewModel.wasLoadedLiveData.value!!)
        outState.putString(ALBUM_IMAGE_PATH_KEY, viewModel.albumImagePathLiveData.value)
        outState.putParcelable(ALBUM_IMAGE_URI_KEY, viewModel.albumImageUriLiveData.value)
        outState.putSerializable(TRACK_LIST_KEY, viewModel.trackListLiveData.value!!.toTypedArray())
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_change_track, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val act = requireActivity() as MainActivity

        when (item.itemId) {
            R.id.accept_change -> viewModel.viewModelScope.launch {
                launch(Dispatchers.IO) {
                    val track = Track(
                        track.androidId,
                        binding.trackAlbumChangeInput.text.toString(),
                        binding.trackArtistChangeInput.text.toString(),
                        binding.trackAlbumChangeInput.text.toString(),
                        track.path,
                        track.duration,
                        track.relativePath,
                        track.displayName,
                        track.addDate
                    )

                    var imageTask: Deferred<Job>? = null

                    val bitmapTarget = object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                        ) {
                            val trackImage = TrackImage(
                                track.path,
                                resource.toByteArray()
                            )

                            imageTask = viewModel.viewModelScope.async(Dispatchers.IO) {
                                val rep = ImageRepository.instance
                                rep.getTrackWithImageAsync(track.path).await()?.let {
                                    rep.updateTrackWithImageAsync(trackImage)
                                } ?: rep.addTrackWithImageAsync(trackImage)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) = Unit
                    }

                    viewModel.albumImagePathLiveData.value?.let {
                        Glide.with(this@TrackChangeFragment)
                            .asBitmap()
                            .load(it)
                            .into(bitmapTarget)
                    } ?: viewModel.albumImageUriLiveData.value?.let {
                        Glide.with(this@TrackChangeFragment)
                            .asBitmap()
                            .load(it)
                            .into(bitmapTarget)
                    }

                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            (act.application as MainApplication)
                                .changedTracks[track.path] = track
                        }

                        else -> {
                            val content = ContentValues().apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                                put(
                                    MediaStore.Audio.Media.TITLE,
                                    binding.trackAlbumChangeInput.text.toString()
                                )

                                put(
                                    MediaStore.Audio.Media.ARTIST,
                                    binding.trackArtistChangeInput.text.toString()
                                )

                                put(
                                    MediaStore.Audio.Media.ALBUM,
                                    binding.trackAlbumChangeInput.text.toString()
                                )
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
                            if ((act.application as MainApplication).curPath == track.path) {
                                imageTask?.await()?.join()
                                act.updateUI(track to false)
                            }
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
                                    binding.trackTitleChangeInput.text.toString(),
                                    binding.trackArtistChangeInput.text.toString(),
                                    binding.trackAlbumChangeInput.text.toString(),
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

                act.supportFragmentManager.popBackStack()
            }

            R.id.update_change -> updateUI()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun up() {
        if (!(requireActivity() as MainActivity).upped)
            binding.trackChangeView.layoutParams =
                (binding.trackChangeView.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = (requireActivity() as MainActivity).playingToolbarHeight
                }
    }

    override fun updateUI(src: Pair<String, String>) {
        HappiFetcher()
            .fetchTrackDataSearch("${src.first} ${src.second}", apiKey)
            .observe(viewLifecycleOwner) {
                GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create()
                    .fromJson(it, HappiFetcher.ParseObject::class.java)
                    .run {
                        viewModel.trackListLiveData.value = mutableListOf<FoundTrack>().apply {
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

                        initRecyclerViews()
                    }
            }
    }

    private fun updateUI() = updateUI(
        binding.trackArtistChangeInput.text.toString() to
                binding.trackTitleChangeInput.text.toString()
    )

    /**
     * Changes album image source
     * @param image Uri of image
     */

    override fun setUserImage(image: Uri) {
        viewModel.albumImagePathLiveData.value = null
        viewModel.albumImageUriLiveData.value = image

        Glide.with(this)
            .load(image)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(binding.currentImage.width, binding.currentImage.height)
            .into(binding.currentImage)
    }

    /**
     * Initialises (or reinitialises) recycler views
     */

    private fun initRecyclerViews() {
        tracksAdapter = TrackAdapter(viewModel.trackListLiveData.value!!).apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        imagesAdapter = ImageAdapter(
            viewModel.trackListLiveData.value!!
                .map(FoundTrack::cover)
                .toMutableList()
                .apply { add(ADD_IMAGE_FROM_STORAGE) }
        ).apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.run {
            similarTracksRecyclerView.adapter = tracksAdapter
            imagesRecyclerView.adapter = imagesAdapter
            emptySimilarTracks.visibility = when {
                this@TrackChangeFragment.viewModel.trackListLiveData.value!!.isEmpty() ->
                    carbon.widget.TextView.VISIBLE
                else -> carbon.widget.TextView.INVISIBLE
            }
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

        inner class TrackHolder(private val trackBinding: ListItemTrackWithoutSettingsBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: FoundTrack

            init {
                itemView.setOnClickListener(this)
                trackBinding.viewModel = TrackItemViewModel()
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(
                    track,
                    binding.trackTitleChangeInput,
                    binding.trackArtistChangeInput,
                    binding.trackAlbumChangeInput
                )
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: FoundTrack) {
                trackBinding.track = _track
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    track = _track
                    trackBinding.trackFoundLyricsNumber.text = (layoutPosition + 1).toString()
                    trackBinding.executePendingBindings()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                ListItemTrackWithoutSettingsBinding.inflate(
                    LayoutInflater.from(parent.context),
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

        inner class ImageHolder(private val imageBinding: ListItemImageBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(imageBinding.root),
            View.OnClickListener {
            private lateinit var image: String

            init {
                itemView.setOnClickListener(this)
                imageBinding.viewModel = ViewModel()
            }

            override fun onClick(v: View?) {
                viewModel.albumImagePathLiveData.value = image
                viewModel.albumImageUriLiveData.value = null

                when (image) {
                    ADD_IMAGE_FROM_STORAGE -> requireActivity().startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        ), ChangeImageFragment.PICK_IMAGE
                    )


                    else -> (callbacker as Callbacks?)?.onImageSelected(
                        ((imageBinding.imageItem.drawable.current) as BitmapDrawable).bitmap,
                        binding.currentImage
                    )
                }
            }

            /**
             * Constructs GUI for image item
             * @param _image track to bind and use
             */

            fun bind(_image: String) {
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    image = _image
                    Glide.with(this@TrackChangeFragment)
                        .run {
                            when (_image) {
                                ADD_IMAGE_FROM_STORAGE -> load(android.R.drawable.ic_menu_gallery)
                                else -> load(_image)
                            }
                        }
                        .placeholder(R.drawable.album_default)
                        .skipMemoryCache(true)
                        .override(imageBinding.imageItem.width, imageBinding.imageItem.height)
                        .into(imageBinding.imageItem)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder =
            ImageHolder(
                ListItemImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = images.size

        override fun onBindViewHolder(holder: ImageHolder, position: Int): Unit =
            holder.bind(images[position])
    }
}