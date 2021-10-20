package com.dinaraparanid.prima.fragments

import android.app.RecoverableSecurityException
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
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentChangeTrackInfoBinding
import com.dinaraparanid.prima.databinding.ListItemImageBinding
import com.dinaraparanid.prima.databinding.ListItemSongBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.extensions.unwrapOr
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.web.genius.Artist
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.utils.web.genius.songs_response.SongsResponse
import com.dinaraparanid.prima.viewmodels.androidx.TrackChangeViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ArtistListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import kotlinx.coroutines.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Fragment to change track's metadata.
 * @since Android 11 it only changes entities in app,
 * but not metadata of track itself
 */

class TrackChangeFragment :
    CallbacksFragment<FragmentChangeTrackInfoBinding>(),
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
            selectedTrack: Song,
            titleInput: EditText,
            artistInput: EditText,
            albumInput: EditText
        )
    }

    private lateinit var track: AbstractTrack
    override var binding: FragmentChangeTrackInfoBinding? = null
    private var imagesAdapter: ImageAdapter? = null
    private var tracksAdapter: TrackAdapter? = null

    internal val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    private val geniusFetcher: GeniusFetcher by lazy { GeniusFetcher() }

    internal companion object {
        private const val ALBUM_IMAGE_PATH_KEY = "album_image_path"
        private const val ALBUM_IMAGE_URI_KEY = "album_image_uri"
        private const val TRACK_LIST_KEY = "track_list"
        private const val WAS_LOADED_KEY = "was_loaded"
        private const val TRACK_KEY = "track"
        private const val ADD_IMAGE_FROM_STORAGE = "add_image_from_storage"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param track track to change
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            track: AbstractTrack,
        ) = TrackChangeFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TRACK_KEY, track)
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentChangeTrackInfoBinding>(
            inflater,
            R.layout.fragment_change_track_info,
            container,
            false
        ).apply {
            viewModel = TrackItemViewModel(0)
            track = this@TrackChangeFragment.track
        }

        viewModel.load(
            savedInstanceState?.getBoolean(WAS_LOADED_KEY),
            savedInstanceState?.getString(ALBUM_IMAGE_PATH_KEY),
            savedInstanceState?.getParcelable(ALBUM_IMAGE_URI_KEY) as Uri?,
            savedInstanceState?.getSerializable(TRACK_LIST_KEY) as Array<Song>?
        )

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            viewModel.albumImagePathLiveData.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .load(it)
                    .into(binding!!.currentImage)
            } ?: viewModel.albumImageUriLiveData.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .load(it)
                    .into(binding!!.currentImage)
            } ?: Glide.with(this@TrackChangeFragment)
                .load(
                    application
                        .getAlbumPictureAsync(track.path, true)
                        .await()
                )
                .into(binding!!.currentImage)
        }

        binding!!.run {
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

        if (application.playingBarIsVisible) up()
        mainActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
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
        when (item.itemId) {
            R.id.accept_change -> viewModel.viewModelScope.launch(Dispatchers.IO) { updateAndSaveTrack() }
            R.id.update_change -> updateUI()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun up() {
        if (!mainActivity.isUpped)
            binding!!.trackChangeView.layoutParams =
                (binding!!.trackChangeView.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    override fun updateUI(src: Pair<String, String>) {
        var cnt = AtomicInteger(1)
        val lock = ReentrantLock()
        val condition = lock.newCondition()
        val tasks = mutableListOf<LiveData<SongsResponse>>()

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                geniusFetcher
                    .fetchTrackDataSearch("${src.first} ${src.second}")
                    .observe(viewLifecycleOwner) { searchResponse ->
                        runBlocking {
                            launch(Dispatchers.IO) {
                                when (searchResponse.meta.status) {
                                    !in 200 until 300 -> {
                                        cnt = AtomicInteger()
                                    }

                                    else -> {
                                        cnt = AtomicInteger(searchResponse.response.hits.size)

                                        searchResponse.response.hits.forEach { data ->
                                            tasks.add(geniusFetcher.fetchTrackInfoSearch(data.result.id))

                                            if (cnt.decrementAndGet() == 0)
                                                lock.withLock(condition::signal)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }.join()

            lock.withLock {
                while (cnt.get() > 0)
                    condition.await()

                launch(Dispatchers.Main) {
                    val trackList = mutableListOf<Song>()
                    val lock2 = ReentrantLock()
                    val condition2 = lock2.newCondition()
                    val cnt2 = AtomicInteger(tasks.size)

                    tasks.forEach { liveData ->
                        liveData.observe(viewLifecycleOwner) { songResponse ->
                            songResponse
                                .takeIf { it.meta.status in 200 until 300 }
                                ?.let { trackList.add(it.response.song) }

                            if (cnt2.decrementAndGet() == 0)
                                lock2.withLock(condition2::signal)
                        }
                    }

                    launch(Dispatchers.IO) {
                        lock2.withLock {
                            while (cnt2.get() > 0)
                                condition2.await()

                            launch(Dispatchers.Main) {
                                viewModel.trackListLiveData.value = trackList
                                initRecyclerViews()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateUI() = updateUI(
        binding!!.trackArtistChangeInput.text.toString() to
                binding!!.trackTitleChangeInput.text.toString()
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
            .override(binding!!.currentImage.width, binding!!.currentImage.height)
            .into(binding!!.currentImage)
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
                .flatMap {
                    listOfNotNull(
                        it.headerImageUrl,
                        it.songArtImageUrl,
                        it.album?.coverArtUrl,
                        it.primaryArtist.imageUrl,
                    ) + it.featuredArtists.map(Artist::imageUrl)
                }
                .distinct()
                .toMutableList()
                .apply { add(ADD_IMAGE_FROM_STORAGE) }
        ).apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding!!.run {
            similarTracksRecyclerView.adapter = tracksAdapter
            imagesRecyclerView.adapter = imagesAdapter
            emptySimilarTracks.visibility = when {
                this@TrackChangeFragment.viewModel.trackListLiveData.value!!.isEmpty() ->
                    carbon.widget.TextView.VISIBLE
                else -> carbon.widget.TextView.INVISIBLE
            }
        }
    }

    private suspend fun updateAndSaveTrack() = coroutineScope {
        var isUpdated = false

        val newTrack = DefaultTrack(
            track.androidId,
            binding!!.trackTitleChangeInput.text.toString(),
            binding!!.trackArtistChangeInput.text.toString(),
            binding!!.trackAlbumChangeInput.text.toString(),
            track.path,
            track.duration,
            track.relativePath,
            track.displayName,
            track.addDate
        )

        application.curPlaylist.run {
            replace(track, newTrack)
            StorageUtil(requireContext().applicationContext).storeCurPlaylist(this)
        }

        val mediaStoreTask = launch(Dispatchers.IO) {
            val lock = ReentrantLock()
            val condition = lock.newCondition()
            var imageTask: Job? = null

            val bitmapTarget = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    val trackImage = TrackImage(newTrack.path, resource.toByteArray())

                    imageTask = viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val rep = ImageRepository.instance
                        rep.removeTrackWithImageAsync(newTrack.path).join()

                        try {
                            rep.addTrackWithImageAsync(trackImage).join()
                            lock.withLock(condition::signal)
                        } catch (e: Exception) {
                            rep.removeTrackWithImageAsync(newTrack.path)

                            viewModel.viewModelScope.launch(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.image_too_big,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            }

            val willImageUpdate = viewModel.albumImagePathLiveData.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .asBitmap()
                    .load(it)
                    .into(bitmapTarget)
                true
            } ?: viewModel.albumImageUriLiveData.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .asBitmap()
                    .load(it)
                    .into(bitmapTarget)
                true
            } ?: false

            val content = ContentValues().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                put(MediaStore.Audio.Media.TITLE, newTrack.title)
                put(MediaStore.Audio.Media.ARTIST, newTrack.artist)
                put(MediaStore.Audio.Media.ALBUM, newTrack.playlist)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // It's properly works only on second time...
                    updateMediaStoreQAsync(content).join()
                    isUpdated = updateMediaStoreQAsync(content).await()
                } catch (securityException: SecurityException) {
                    val recoverableSecurityException = securityException as?
                            RecoverableSecurityException
                        ?: throw RuntimeException(
                            securityException.message,
                            securityException
                        )

                    recoverableSecurityException
                        .userAction
                        .actionIntent
                        .intentSender
                        .let {
                            startIntentSenderForResult(
                                it, 125,
                                null, 0, 0, 0, null
                            )
                        }
                }
            } else {
                isUpdated = true

                mainActivity.contentResolver.update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    content,
                    "${MediaStore.Audio.Media.DATA} = ?",
                    arrayOf(newTrack.path)
                )
            }

            runBlocking {
                launch(Dispatchers.IO) {
                    if (application.curPath == newTrack.path)
                        lock.withLock {
                            while (imageTask == null && willImageUpdate)
                                condition.await()

                            launch(Dispatchers.Main) { mainActivity.updateUI(newTrack to false) }
                        }
                }.join()
            }
        }

        launch(Dispatchers.IO) {
            CustomPlaylistsRepository.instance.getTrackAsync(newTrack.path).await()
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
                            newTrack.title,
                            newTrack.artist,
                            newTrack.playlist,
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

        mediaStoreTask.join()

        if (isUpdated) {
            requireActivity().sendBroadcast(Intent(MainActivity.Broadcast_UPDATE_NOTIFICATION))
            mainActivity.supportFragmentManager.popBackStack()
        }
    }

    /**
     * Updates columns in MediaStore for Android Api 29+
     * @param content new columns to set
     */

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateMediaStoreQAsync(content: ContentValues): Deferred<Boolean> {
        val act = requireActivity()
        val resolver = act.contentResolver

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        resolver.update(
            uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }, null, null
        )

        resolver.update(
            uri,
            content,
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(track.androidId.toString())
        )

        val upd = {
            try {
                AudioFileIO.read(File(track.path)).run {
                    tag.run {
                        setField(
                            FieldKey.TITLE,
                            binding!!.trackTitleChangeInput.text.toString()
                        )

                        setField(
                            FieldKey.ARTIST,
                            binding!!.trackArtistChangeInput.text.toString()
                        )

                        setField(
                            FieldKey.ALBUM,
                            binding!!.trackAlbumChangeInput.text.toString()
                        )
                    }

                    commit()
                }

                true
            } catch (e: Exception) {
                false
            }
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                viewModel.viewModelScope.async(Dispatchers.Main) {
                    application
                        .checkAndRequestManageExternalStoragePermission { upd() }
                        .unwrapOr(false)
                }

            else -> viewModel.viewModelScope.async(Dispatchers.IO) { upd() }
        }
    }

    /**
     * [androidx.recyclerview.widget.RecyclerView.Adapter]
     * for [TrackChangeFragment] (tracks)
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<Song>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(private val trackBinding: ListItemSongBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: Song

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(
                    track,
                    binding!!.trackTitleChangeInput,
                    binding!!.trackArtistChangeInput,
                    binding!!.trackAlbumChangeInput
                )
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: Song) {
                track = _track
                trackBinding.track = _track
                trackBinding.viewModel = TrackItemViewModel(layoutPosition + 1)
                trackBinding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                ListItemSongBinding.inflate(
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
                imageBinding.viewModel = ArtistListViewModel()
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
                        (imageBinding.imageItem.drawable.current as BitmapDrawable).bitmap,
                        binding!!.currentImage
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