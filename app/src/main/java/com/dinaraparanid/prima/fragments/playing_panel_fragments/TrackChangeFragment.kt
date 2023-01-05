package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.covers.TrackCover
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.FragmentChangeTrackInfoBinding
import com.dinaraparanid.prima.databinding.ListItemImageBinding
import com.dinaraparanid.prima.databinding.ListItemSongBinding
import com.dinaraparanid.prima.dialogs.MessageDialog
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.entities.Track
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.*
import com.dinaraparanid.prima.utils.web.genius.Artist
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.utils.web.genius.songs_response.SongsResponse
import com.dinaraparanid.prima.mvvmp.androidx.TrackChangeViewModel
import com.dinaraparanid.prima.mvvmp.old_shit.ArtistListViewModel
import com.dinaraparanid.prima.mvvmp.view.fragments.*
import com.dinaraparanid.prima.mvvmp.view.fragments.MainActivityFragmentImpl
import com.dinaraparanid.prima.mvvmp.view.fragments.setMainLabelInitializedSync
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Fragment to change track's metadata */

class TrackChangeFragment :
    CallbacksFragment<FragmentChangeTrackInfoBinding, MainActivity>(),
    Rising,
    UIUpdatable<Pair<String, String>>,
    ChangeImageFragment,
    MainActivityFragment by MainActivityFragmentImpl(),
    MenuProviderFragment,
    AsyncContext,
    StatisticsUpdatable {
    internal interface Callbacks : CallbacksFragment.CallbackHandler {
        /**
         * Makes selected image new track's album image
         * @param image image to select
         * @param albumImageView album image view which image should be replaced
         */

        fun onImageSelected(image: Bitmap, albumImageView: ImageView)

        /**
         * Makes changeable track's metadata
         * equal to selected found track's metadata
         * @param selectedTrack track to select
         * @param titleInput [EditText] for title
         * @param artistInput [EditText] for artist
         * @param albumInput [EditText] for album image
         * @param numberInAlbumInput [EditText] for track's number in album
         */

        suspend fun onTrackSelected(
            selectedTrack: Song,
            titleInput: EditText,
            artistInput: EditText,
            albumInput: EditText,
            numberInAlbumInput: EditText
        )
    }

    override val mutex = Mutex()
    override var binding: FragmentChangeTrackInfoBinding? = null
    override val updateStyle = Statistics::withIncrementedNumberOfChanged
    override val menuProvider = defaultMenuProvider

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    private lateinit var track: Track
    private var awaitDialog: Deferred<KProgressHUD>? = null

    private inline val isPlaying
        get() = application.musicPlayer?.isPlaying

    private suspend fun getCurPath() = StorageUtil.getInstanceAsyncSynchronized().loadTrackPathLocking()

    private val imagesAdapter by lazy {
        ImageAdapter().apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    private val tracksAdapter by lazy {
        TrackAdapter().apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    private val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    private val geniusFetcher by lazy { GeniusFetcher() }

    private val securityExceptionIntentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result != null) updateAndSaveTrackAsync()
        }

    internal companion object {
        private const val ALBUM_IMAGE_PATH_KEY = "album_image_path"
        private const val ALBUM_IMAGE_URI_KEY = "album_image_uri"
        private const val TITLE_KEY = "title"
        private const val ARTIST_KEY = "artist"
        private const val ALBUM_KEY = "album"
        private const val TRACK_NUMBER_IN_ALBUM_KEY = "track_number_in_album"
        private const val TRACK_LIST_KEY = "track_list"
        private const val WAS_LOADED_KEY = "was_loaded"
        private const val TRACK_KEY = "track"
        private const val ADD_IMAGE_FROM_STORAGE = "add_image_from_storage"

        /**
         * Creates new instance of fragment with params
         * @param track track to change
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(track: Track) = TrackChangeFragment().apply {
            arguments = Bundle().apply { putSerializable(TRACK_KEY, track) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        track = requireArguments().getSerializable(TRACK_KEY) as Track
        mainLabelText.set(resources.getString(R.string.change_track_s_information))

        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
        setMainActivityMainLabel()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.load(
            savedInstanceState?.getBoolean(WAS_LOADED_KEY),
            savedInstanceState?.getString(ALBUM_IMAGE_PATH_KEY),
            savedInstanceState?.getParcelable(ALBUM_IMAGE_URI_KEY) as Uri?,
            savedInstanceState?.getString(TITLE_KEY) ?: track.title,
            savedInstanceState?.getString(ARTIST_KEY) ?: track.artist,
            savedInstanceState?.getString(ALBUM_KEY) ?: track.album,
            savedInstanceState?.getByte(TRACK_NUMBER_IN_ALBUM_KEY) ?: track.trackNumberInAlbum,
            savedInstanceState?.getSerializable(TRACK_LIST_KEY) as Array<Song>?
        )

        binding = DataBindingUtil.inflate<FragmentChangeTrackInfoBinding>(
            inflater,
            R.layout.fragment_change_track_info,
            container,
            false
        ).apply {
            viewModel = TrackItemViewModel(_pos = 0, track)
            title = this@TrackChangeFragment.viewModel.titleFlow.value
            artist = this@TrackChangeFragment.viewModel.artistFlow.value
            album = this@TrackChangeFragment.viewModel.albumFlow.value
            numberInAlbum = this@TrackChangeFragment.viewModel.trackNumberInAlbumFlow.value
        }

        setCurrentImageAsync()

        binding!!.run {
            trackTitleChangeInput.setHintTextColor(Color.GRAY)
            trackArtistChangeInput.setHintTextColor(Color.GRAY)
            trackAlbumChangeInput.setHintTextColor(Color.GRAY)
            trackPosChangeInput.setHintTextColor(Color.GRAY)

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

                runOnUIThread { imagesAdapter.setCurrentList(listOf(ADD_IMAGE_FROM_STORAGE)) }
                adapter = imagesAdapter
                addItemDecoration(HorizontalSpaceItemDecoration(30))
            }
        }

        when {
            viewModel.wasLoadedFlow.value -> initRecyclerViews()

            else -> {
                viewModel.wasLoadedFlow.value = true
                runOnUIThread { updateUIAsync(track.artist to track.title, isLocking = true) }
            }
        }

        binding!!.imagesRecyclerView.scrollToPosition(0)
        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_change_track, menu)

        fragmentActivity.run {
            runOnUIThread {
                while (!isMainLabelInitialized.get())
                    awaitMainLabelInitCondition.blockAsync()

                launch(Dispatchers.Main) {
                    mainLabelText = this@TrackChangeFragment.mainLabelText.get()
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.accept_change -> updateAndSaveTrackAsync()

            R.id.update_change -> runOnUIThread {
                val drawableWrapper = AnimationDrawableWrapper(
                    requireActivity().resources,
                    menuItem.icon!!
                )

                menuItem.icon = drawableWrapper

                val animator = ObjectAnimator.ofInt(0, 360).apply {
                    addUpdateListener { drawableWrapper.setRotation(it) }
                    start()
                }

                updateUIAsync(isLocking = true)
                animator.cancel()
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(WAS_LOADED_KEY, viewModel.wasLoadedFlow.value)
        outState.putString(ALBUM_IMAGE_PATH_KEY, viewModel.albumImageUrlFlow.value)
        outState.putParcelable(ALBUM_IMAGE_URI_KEY, viewModel.albumImagePathFlow.value)
        outState.putString(TITLE_KEY, binding?.trackTitleChangeInput?.text.toString())
        outState.putString(ARTIST_KEY, binding?.trackArtistChangeInput?.text.toString())
        outState.putString(ALBUM_KEY, binding?.trackAlbumChangeInput?.text.toString())
        outState.putSerializable(TRACK_LIST_KEY, viewModel.trackListFlow.value.toTypedArray())
        outState.putByte(
            TRACK_NUMBER_IN_ALBUM_KEY,
            binding?.trackPosChangeInput?.text?.toString()?.toByteOrNull() ?: -1
        )
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    override fun onStop() {
        super.onStop()
        freeUI()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
        setCurrentImageAsync()
    }

    override fun onDestroyView() {
        freeUI()
        super.onDestroyView()

        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.trackChangeView.layoutParams =
                (binding!!.trackChangeView.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    /** Refreshes UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: Pair<String, String>) = coroutineScope {
        runOnIOThread {
            val cnt = AtomicInteger(1)
            val condition = AsyncCondVar()
            val tasks = mutableListOf<LiveData<SongsResponse>>()

            launch(Dispatchers.Main) {
                geniusFetcher
                    .fetchTrackDataSearch("${src.first} ${src.second}")
                    .observe(viewLifecycleOwner) { searchResponse ->
                        runOnWorkerThread {
                            when (searchResponse.meta.status) {
                                !in 200 until 300 -> cnt.set(0)

                                else -> {
                                    cnt.set(searchResponse.response.hits.size)

                                    searchResponse.response.hits
                                        .takeIf(Array<*>::isNotEmpty)
                                        ?.forEach { data ->
                                            tasks.add(
                                                geniusFetcher
                                                    .fetchTrackInfoSearch(data.result.id)
                                            )

                                            if (cnt.decrementAndGet() == 0)
                                                condition.openAsync()
                                        }
                                        ?: condition.openAsync()
                                }
                            }
                        }
                    }
            }.join()

            while (cnt.get() > 0)
                condition.blockAsync()

            launch(Dispatchers.Main) {
                val trackList = mutableListOf<Song>()
                val condition2 = AsyncCondVar()
                val cnt2 = AtomicInteger(tasks.size)

                tasks.takeIf(List<*>::isNotEmpty)?.forEach { liveData ->
                    val isObservingStarted = AtomicBoolean()
                    val itemCondition = AsyncCondVar()

                    liveData.observe(viewLifecycleOwner) { songResponse ->
                        runOnUIThread {
                            isObservingStarted.set(true)
                            itemCondition.openAsync()

                            songResponse
                                .takeIf { it.meta.status in 200 until 300 }
                                ?.let { trackList.add(it.response.song) }

                            if (cnt2.decrementAndGet() == 0)
                                condition2.openAsync()
                        }
                    }

                    launch(Dispatchers.IO) {
                        if (!isObservingStarted.get())
                            itemCondition.blockAsync(5000)

                        if (!isObservingStarted.get()) {
                            if (cnt2.decrementAndGet() == 0)
                                condition2.openAsync()
                        }
                    }

                } ?: run {
                    cnt2.set(0)
                    condition2.blockAsync()
                }

                launch(Dispatchers.IO) {
                    while (cnt2.get() > 0)
                        condition2.blockAsync()

                    launch(Dispatchers.Main) {
                        viewModel.trackListFlow.value = trackList
                        initRecyclerViews()
                    }
                }.join()
            }
        }.join()
    }

    /**
     * Changes album image source
     * @param image Uri of image
     */

    override suspend fun setUserImageAsync(image: Uri) = runOnUIThread {
        viewModel.albumImageUrlFlow.value = null
        viewModel.albumImagePathFlow.value = image
        val curImage = binding!!.currentImage

        Glide.with(this@TrackChangeFragment)
            .load(image)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(curImage.width, curImage.height)
            .into(curImage)
    }

    private suspend fun updateUIAsync(isLocking: Boolean) = updateUIAsync(
        binding!!.trackArtistChangeInput.text.toString() to
                binding!!.trackTitleChangeInput.text.toString(),
        isLocking
    )

    /** Initialises (or reinitialises) recycler views */

    private fun initRecyclerViews() = runOnUIThread {
        imagesAdapter.setCurrentList(
            viewModel.trackListFlow.value
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
        )

        tracksAdapter.setCurrentList(viewModel.trackListFlow.value)

        binding!!.run {
            similarTracksRecyclerView.adapter = tracksAdapter
            similarTracksRecyclerView.scrollToPosition(0)

            imagesRecyclerView.adapter = imagesAdapter
            imagesRecyclerView.scrollToPosition(0)

            emptySimilarTracks.visibility = when {
                this@TrackChangeFragment.viewModel.trackListFlow.value.isEmpty() ->
                    carbon.widget.TextView.VISIBLE
                else -> carbon.widget.TextView.INVISIBLE
            }
        }
    }

    /** Updates tags and all databases asynchronously */
    private fun updateAndSaveTrackAsync(): Job = runOnIOThread {
        var isUpdated = false

        val path = track.path
        val newTitle = binding!!.trackTitleChangeInput.text.toString()
        val newArtist = binding!!.trackArtistChangeInput.text.toString()
        val newAlbum = binding!!.trackAlbumChangeInput.text.toString()
        val newNumberInAlbum = binding!!.trackPosChangeInput.text.toString().toByteOrNull()
            ?.let { if (it < -1) -1 else it } ?: -1

        val newTrack = DefaultTrack(
            track.androidId,
            newTitle,
            newArtist,
            newAlbum,
            path,
            track.duration,
            track.relativePath,
            track.displayName,
            track.addDate,
            newNumberInAlbum
        )

        FavouriteRepository
            .getInstanceSynchronized()
            .updateTrackAsync(path, newTitle, newArtist, newAlbum, newNumberInAlbum)

        CustomPlaylistsRepository
            .getInstanceSynchronized()
            .updateTracksAsync(path, newTitle, newArtist, newAlbum, newNumberInAlbum)

        StatisticsRepository
            .getInstanceSynchronized()
            .updateTrackAsync(path, newTitle, newArtist, newAlbum, newNumberInAlbum)

        StorageUtil
            .getInstanceAsyncSynchronized()
            .storeCurPlaylistLocking(application.curPlaylist.apply { replace(track, newTrack) })

        val updateTask = launch(Dispatchers.IO) {
            val content = ContentValues().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                put(MediaStore.Audio.Media.TITLE, newTrack.title)
                put(MediaStore.Audio.Media.ARTIST, newTrack.artist)
                put(MediaStore.Audio.Media.ALBUM, newTrack.album)
                put(MediaStore.Audio.Media.TRACK, newTrack.trackNumberInAlbum)
            }

            try {
                fragmentActivity.contentResolver.update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    content,
                    "${MediaStore.Audio.Media.DATA} = ?",
                    arrayOf(newTrack.path)
                )
            } catch (ignored: Exception) {
                // Android 10+ Error
            }

            try {
                awaitDialog = async(Dispatchers.Main) {
                    createAndShowAwaitDialog(
                        context = requireContext(),
                        isCancelable = false
                    )
                }

                val wasPlaying = try { isPlaying ?: false } catch (e: Exception) { false }

                val resumeTime = try {
                    application.musicPlayer?.currentPosition
                        ?: StorageUtil.getInstanceAsyncSynchronized().loadTrackPauseTimeLocking()
                } catch (e: Exception) {
                    StorageUtil.getInstanceAsyncSynchronized().loadTrackPauseTimeLocking()
                }

                if (wasPlaying && getCurPath() == track.path)
                    fragmentActivity.pausePlaying(isLocking = true, isUiUpdating = true)

                isUpdated = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        updateTrackFileTagsAsync(content).await()

                    else -> {
                        val task = CoversRepository
                            .getInstanceSynchronized()
                            .removeTrackWithCoverAsync(path)

                        val bitmapTarget = object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {
                                try {
                                    val stream = ByteArrayOutputStream()
                                    resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val byteArray = stream.toByteArray()

                                    runOnIOThread {
                                        task.join()

                                        CoversRepository
                                            .getInstanceSynchronized()
                                            .addTracksWithCoversAsync(TrackCover(path, byteArray))
                                    }
                                } catch (e: Exception) {
                                    runOnUIThread {
                                        MessageDialog(R.string.image_not_supported)
                                            .show(parentFragmentManager, null)
                                    }
                                }
                            }

                            override fun onLoadCleared(placeholder: Drawable?) = Unit
                        }

                        viewModel.albumImageUrlFlow.value?.let {
                            Glide.with(this@TrackChangeFragment)
                                .asBitmap()
                                .load(it)
                                .into(bitmapTarget)
                        } ?: viewModel.albumImagePathFlow.value?.let {
                            Glide.with(this@TrackChangeFragment)
                                .asBitmap()
                                .load(it)
                                .into(bitmapTarget)
                        }

                        true
                    }
                }

                if (wasPlaying && getCurPath() == track.path)
                    fragmentActivity.restartPlayingAfterTrackChangedLocked(resumeTime)

                runOnUIThread { awaitDialog?.await()?.dismiss() }
                application.scanSingleFile(track.path)
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw RuntimeException(securityException.message, securityException)

                    recoverableSecurityException
                        .userAction
                        .actionIntent
                        .intentSender
                        .let { intentSender ->
                            securityExceptionIntentResultLauncher.launch(
                                IntentSenderRequest.Builder(intentSender).build()
                            )
                        }
                }
            }

            launch(Dispatchers.Main) {
                if (getCurPath() == newTrack.path)
                    fragmentActivity.updateUIAsync(track to newTrack, isLocking = true)
            }
        }

        updateStatisticsAsync()
        updateTask.join()

        if (isUpdated) {
            requireActivity().sendBroadcast(Intent(MainActivity.Broadcast_UPDATE_NOTIFICATION))
            fragmentActivity.supportFragmentManager.popBackStack()
        }
    }

    /**
     * Updates columns in MediaStore
     * @param content new columns to set
     * @return true if file was updated, false otherwise
     */

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTrackFileTagsAsync(content: ContentValues): Deferred<Boolean> {
        val resolver = requireActivity().contentResolver

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            resolver.update(
                uri, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }, null, null
            )

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            resolver.update(
                uri,
                content,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(track.androidId.toString())
            )

        suspend fun updFile() = try {
            AudioFileIO.read(File(track.path)).run {
                val condition = AsyncCondVar()
                val isUpdated = AtomicBoolean()

                tagOrCreateAndSetDefault?.let { tag ->
                    val bitmapTarget = object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                        ) {
                            try {
                                val stream = ByteArrayOutputStream()
                                resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                val byteArray = stream.toByteArray()
                                tagOrCreateAndSetDefault.deleteArtworkField()

                                tagOrCreateAndSetDefault.setField(
                                    ArtworkFactory
                                        .createArtworkFromFile(File(track.path))
                                        .apply { binaryData = byteArray }
                                )
                            } catch (e: Exception) {
                                runOnUIThread {
                                    MessageDialog(R.string.image_not_supported)
                                        .show(parentFragmentManager, null)
                                }
                            }

                            runOnWorkerThread {
                                isUpdated.set(true)
                                condition.openAsync()
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            runOnWorkerThread {
                                isUpdated.set(true)
                                condition.openAsync()
                            }
                        }
                    }

                    isUpdated.set(
                        viewModel.albumImageUrlFlow.value == null &&
                                viewModel.albumImagePathFlow.value == null
                    )

                    viewModel.albumImageUrlFlow.value?.let {
                        Glide.with(requireActivity())
                            .asBitmap()
                            .load(it)
                            .into(bitmapTarget)
                    } ?: viewModel.albumImagePathFlow.value?.let {
                        Glide.with(requireActivity())
                            .asBitmap()
                            .load(it)
                            .into(bitmapTarget)
                    }

                    tag.setField(
                        FieldKey.TITLE,
                        binding!!.trackTitleChangeInput.text.toString()
                    )

                    tag.setField(
                        FieldKey.ARTIST,
                        binding!!.trackArtistChangeInput.text.toString()
                    )

                    tag.setField(
                        FieldKey.ALBUM,
                        binding!!.trackAlbumChangeInput.text.toString()
                    )

                    tag.setField(
                        FieldKey.TRACK,
                        "${
                            binding!!.trackPosChangeInput.text.toString().toByteOrNull()
                                ?.let { if (it < -1) -1 else it } ?: -1
                        }"
                    )
                }

                while (!isUpdated.get())
                    condition.blockAsync()
                commit()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()

            runOnUIThread {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.failure)
                    .setMessage(e.message ?: resources.getString(R.string.unknown_error))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .setCancelable(false)
                    .show()
            }

            false
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                getFromUIThreadAsync {
                    application
                        .checkAndRequestManageExternalStoragePermission {
                            getFromIOThreadAsync { updFile() }.await()
                        } ?: false
                }

            else -> getFromIOThreadAsync { updFile() }
        }
    }

    /** Sets current image by url, or path, or track's cover */
    private fun setCurrentImageAsync() = runOnUIThread {
        val width = binding!!.currentImage.width
        val height = binding!!.currentImage.height

        Glide.with(this@TrackChangeFragment)
            .load(
                viewModel.albumImageUrlFlow.value
                    ?: viewModel.albumImagePathFlow.value
                    ?: application
                        .getAlbumPictureAsync(track.path)
                        .await()
            )
            .placeholder(R.drawable.album_default)
            .fallback(R.drawable.album_default)
            .error(R.drawable.album_default)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(width, height)
            .into(binding!!.currentImage)
    }

    private fun freeUI() {
        binding?.currentImage?.let(Glide.with(this)::clear)

        Glide.get(requireContext()).run {
            runOnIOThread { clearDiskCache() }
            bitmapPool.clearMemory()
            clearMemory()
        }
    }

    /** [AsyncListDifferAdapter] for [TrackChangeFragment] (tracks) */
    inner class TrackAdapter : AsyncListDifferAdapter<Song, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(first: Song, second: Song) = first == second

        /** [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter] */
        inner class TrackHolder(private val trackBinding: ListItemSongBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: Song

            init {
                itemView.setOnClickListener(this)
            }

            @SuppressLint("SyntheticAccessor")
            override fun onClick(v: View?) {
                runOnUIThread {
                    awaitDialog = async(Dispatchers.Main) {
                        createAndShowAwaitDialog(requireContext(), isCancelable = false)
                    }

                    (callbackHandler as Callbacks?)?.onTrackSelected(
                        track,
                        binding!!.trackTitleChangeInput,
                        binding!!.trackArtistChangeInput,
                        binding!!.trackAlbumChangeInput,
                        binding!!.trackPosChangeInput
                    )

                    val curImage = binding!!.currentImage
                    viewModel.albumImageUrlFlow.value = track.songArtImageUrl
                    viewModel.albumImagePathFlow.value = null

                    Glide.with(this@TrackChangeFragment)
                        .asDrawable()
                        .load(track.songArtImageUrl)
                        .placeholder(R.drawable.album_default)
                        .error(R.drawable.album_default)
                        .fallback(R.drawable.album_default)
                        .skipMemoryCache(true)
                        .override(curImage.width, curImage.height)
                        .into(curImage)

                    awaitDialog?.await()?.dismiss()
                }
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            internal fun bind(_track: Song) {
                track = _track.apply {
                    album
                        ?.takeIf { it.name == "null" }
                        ?.run { name = resources.getString(R.string.unknown_album) }
                }

                trackBinding.track = _track
                trackBinding.viewModel = TrackItemViewModel(_pos = layoutPosition + 1)
                trackBinding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackHolder(
            ListItemSongBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: TrackHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }

    /** [AsyncListDifferAdapter] for [TrackChangeFragment] (images) */
    inner class ImageAdapter : AsyncListDifferAdapter<String, ImageAdapter.ImageHolder>() {
        override fun areItemsEqual(first: String, second: String) = first == second

        /** ViewHolder for tracks of [TrackAdapter] */
        inner class ImageHolder(private val imageBinding: ListItemImageBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(imageBinding.root),
            View.OnClickListener {
            private lateinit var image: String

            init {
                itemView.setOnClickListener(this)
                imageBinding.viewModel = ArtistListViewModel()
            }

            @SuppressLint("SyntheticAccessor")
            override fun onClick(v: View?) {
                viewModel.albumImageUrlFlow.value = image
                viewModel.albumImagePathFlow.value = null

                when (image) {
                    ADD_IMAGE_FROM_STORAGE -> fragmentActivity.pickImageIntentResultListener.launch(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                    )

                    else -> (callbackHandler as Callbacks?)?.onImageSelected(
                        image = (imageBinding.imageItem.drawable.current as BitmapDrawable).bitmap,
                        albumImageView = binding!!.currentImage
                    )
                }
            }

            /**
             * Constructs GUI for image item
             * @param _image track to bind and use
             */

            internal fun bind(_image: String) {
                image = _image

                runOnUIThread {
                    Glide.with(this@TrackChangeFragment)
                        .run {
                            when (image) {
                                ADD_IMAGE_FROM_STORAGE -> load(
                                    when {
                                        Params.getInstanceSynchronized().secondaryColor != -1 ->
                                            when (Params.getInstanceSynchronized().secondaryColor) {
                                                0 -> R.drawable.image_icon_day
                                                else -> R.drawable.image_icon_night
                                            }

                                        Params.getInstanceSynchronized().theme.isNight ->
                                            R.drawable.image_icon_night

                                        else -> R.drawable.image_icon_day
                                    }
                                )
                                else -> load(image)
                            }
                        }
                        .placeholder(R.drawable.album_default)
                        .skipMemoryCache(true)
                        .error(R.drawable.album_default)
                        .fallback(R.drawable.album_default)
                        .override(imageBinding.imageItem.width, imageBinding.imageItem.height)
                        .listener(object : RequestListener<Drawable> {
                            @SuppressLint("SyntheticAccessor")
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                runOnUIThread {
                                    val ind = imagesAdapter.currentList.indexOf(image)
                                    imagesAdapter.setCurrentList(imagesAdapter.currentList - image)
                                    imagesAdapter.notifyItemChanged(ind)
                                }

                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ) = false

                        })
                        .into(imageBinding.imageItem)
                }
            }
        }

        override fun getItemId(position: Int) = position.toLong()
        override fun getItemViewType(position: Int) = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImageHolder(
            ListItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: ImageHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}