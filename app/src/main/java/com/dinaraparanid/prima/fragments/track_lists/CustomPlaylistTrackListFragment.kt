package com.dinaraparanid.prima.fragments.track_lists

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.images.PlaylistImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.RenamePlaylistDialog
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.CustomPlaylistTrackListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.*

/** [AbstractTrackListFragment] for user's playlists */

class CustomPlaylistTrackListFragment :
    TrackCollectionTrackListFragment<FragmentCustomPlaylistTrackListBinding>(),
    ChangeImageFragment {
    private var playlistId = 0L
    private var awaitDialog: KProgressHUD? = null
    internal val playlistTitle by lazy { mainLabelCurText }

    override var binding: FragmentCustomPlaylistTrackListBinding? = null
    override var updater: SwipeRefreshLayout? = null
    override var emptyTextView: TextView? = null
    override var amountOfTracks: carbon.widget.TextView? = null
    override var trackOrderTitle: carbon.widget.TextView? = null
    override val addPlaylistToFavouritesButton get() = binding!!.addPlaylistToFavouritesButton

    internal companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelCurText main label text for current fragment
         * @param playlistId id of playlist
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelCurText: String,
            playlistId: Long,
        ) = CustomPlaylistTrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
    }

    private suspend fun loadImages() {
        initGlideAsync().run {
            val image = binding!!.customPlaylistTracksImage
            override(image.width, image.height).into(image)
        }

        if (!Params.getInstanceSynchronized().isCustomTheme) {
            val imageLayout = binding!!.customPlaylistTracksImageLayout
            initGlideAsync()
                .override(imageLayout.width, imageLayout.height)
                .transform(BlurTransformation(15, 5))
                .into(object : CustomViewTarget<ConstraintLayout, Drawable>(imageLayout) {
                    override fun onLoadFailed(errorDrawable: Drawable?) = Unit
                    override fun onResourceCleared(placeholder: Drawable?) = Unit

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) { imageLayout.background = resource }
                })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentCustomPlaylistTrackListBinding>(
                inflater,
                R.layout.fragment_custom_playlist_track_list,
                container,
                false
            )
            .apply {
                viewModel = CustomPlaylistTrackListViewModel(
                    playlistTitle = playlistTitle,
                    fragment = this@CustomPlaylistTrackListFragment,
                    playlistId = playlistId,
                    itemListGetter = suspend {
                        loadAsync().join()
                        itemList.tracks
                    }
                )

                runOnUIThread {
                    addPlaylistToFavouritesButton.setImageResource(
                        when {
                            viewModel!!.isPlaylistLikedAsync().await() ->
                                R.drawable.heart_like_white
                            else -> R.drawable.heart_white
                        }
                    )
                }

                updater = customPlaylistTrackSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        try {
                            runOnUIThread {
                                loadAsync().await()
                                updateUIAsync(isLocking = true)
                                isRefreshing = false
                            }
                        } catch (ignored: Exception) {
                            // permissions not given
                        }
                    }
                }

                emptyTextView = customPlaylistTrackListEmpty
                trackOrderTitle = customPlaylistTrackOrderTitle

                try {
                    runOnUIThread {
                        val task = loadAsync()
                        awaitDialog = createAndShowAwaitDialog(requireContext(), false)

                        task.await()
                        awaitDialog?.dismiss()
                        initAdapter()

                        setEmptyTextViewVisibility(itemList)
                        itemListSearch.addAll(itemList)
                        adapter.setCurrentList(itemList)
                        loadImages()

                        amountOfTracks = amountOfTracksCustomPlaylist.apply {
                            isSelected = true
                            val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                            text = txt
                        }

                        recyclerView = customPlaylistTrackRecyclerView.apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = this@CustomPlaylistTrackListFragment.adapter
                            addItemDecoration(VerticalSpaceItemDecoration(30))
                        }

                        if (application.playingBarIsVisible) up()
                    }
                } catch (ignored: Exception) {
                    // permissions not given
                }

                amountOfTracks = amountOfTracksCustomPlaylist.apply {
                    isSelected = true
                    val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                    text = txt
                }

                trackOrderTitle = customPlaylistTrackOrderTitle
                updateOrderTitle()
            }

        fragmentActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onPause() {
        super.onPause()
        binding?.customPlaylistTracksImage?.let(Glide.with(this)::clear)
    }

    override fun onResume() {
        super.onResume()
        runOnUIThread { loadImages() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
        binding?.customPlaylistTracksImage?.let(Glide.with(this)::clear)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_custom_playlist_menu, menu)

        (menu.findItem(R.id.cp_search).actionView as SearchView).run {
            setOnQueryTextListener(this@CustomPlaylistTrackListFragment)
        }

        menu.findItem(R.id.cp_find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rename_playlist -> RenamePlaylistDialog(this)
                .show(requireActivity().supportFragmentManager, null)

            R.id.remove_playlist -> AreYouSureDialog(
                R.string.ays_remove_playlist,
            ) {
                runOnIOThread {
                    StatisticsRepository
                        .getInstanceSynchronized()
                        .removeCustomPlaylistAsync(title = mainLabelCurText)

                    ImageRepository
                        .getInstanceSynchronized()
                        .removePlaylistWithImageAsync(title = mainLabelCurText)

                    FavouriteRepository
                        .getInstanceSynchronized()
                        .getPlaylistAsync(
                            title = mainLabelCurText,
                            type = AbstractPlaylist.PlaylistType.CUSTOM.ordinal
                        )
                        .await()
                        ?.let {
                            FavouriteRepository
                                .getInstanceSynchronized()
                                .removePlaylistAsync(it)
                        }
                }

                CustomPlaylistsRepository.getInstanceSynchronized().run {
                    runOnIOThread {
                        removePlaylistAsync(title = mainLabelCurText)
                        removeTracksOfPlaylistAsync(title = mainLabelCurText)
                    }
                }

                requireActivity().supportFragmentManager.popBackStack()
            }.show(requireActivity().supportFragmentManager, null)
        }

        return super.onOptionsItemSelected(item)
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            val task = CustomPlaylistsRepository
                .getInstanceSynchronized()
                .getTracksOfPlaylistAsync(playlistTitle = mainLabelCurText)

            itemList.clear()
            itemList.addAll(Params.sortedTrackList(task.await().enumerated()))
            Unit
        }
    }

    override fun setUserImage(image: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(image)
            .skipMemoryCache(true)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val playlistImage = PlaylistImage(
                            playlistTitle,
                            resource.toByteArray()
                        )

                        runOnIOThread {
                            ImageRepository
                                .getInstanceSynchronized()
                                .removePlaylistWithImageAsync(playlistTitle)
                                .join()

                            try {
                                ImageRepository
                                    .getInstanceSynchronized()
                                    .addPlaylistWithImageAsync(playlistImage)

                                launch(Dispatchers.Main) {
                                    Glide.with(this@CustomPlaylistTrackListFragment)
                                        .load(image)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(
                                            binding!!.customPlaylistTracksImage.width,
                                            binding!!.customPlaylistTracksImage.height
                                        )
                                        .into(binding!!.customPlaylistTracksImage)

                                    if (!Params.getInstanceSynchronized().isCustomTheme)
                                        Glide.with(this@CustomPlaylistTrackListFragment)
                                            .load(image)
                                            .skipMemoryCache(true)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .override(
                                                binding!!.customPlaylistTracksImageLayout.width,
                                                binding!!.customPlaylistTracksImageLayout.height
                                            )
                                            .transform(BlurTransformation(15, 5))
                                            .into(
                                                object : CustomViewTarget<ConstraintLayout, Drawable>(
                                                    binding!!.customPlaylistTracksImageLayout
                                                ) {
                                                    override fun onLoadFailed(errorDrawable: Drawable?) = Unit
                                                    override fun onResourceCleared(placeholder: Drawable?) = Unit

                                                    override fun onResourceReady(
                                                        resource: Drawable,
                                                        transition: Transition<in Drawable>?
                                                    ) {
                                                        binding!!
                                                            .customPlaylistTracksImageLayout
                                                            .background = resource
                                                    }
                                                }
                                            )
                                }
                            } catch (e: Exception) {
                                ImageRepository
                                    .getInstanceSynchronized()
                                    .removePlaylistWithImageAsync(playlistTitle)

                                Toast.makeText(
                                    requireContext(),
                                    R.string.image_too_big,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                }
            )
    }

    /**
     * Renames main label when playlist is rename
     * @param title new playlist's title
     */

    internal fun renameTitle(title: String) {
        mainLabelCurText = title
        fragmentActivity.mainLabelCurText = mainLabelCurText
    }

    private suspend fun initGlideAsync() = Glide.with(this@CustomPlaylistTrackListFragment)
        .load(
            application.run {
                try {
                    val repImage = ImageRepository
                        .getInstanceSynchronized()
                        .getPlaylistWithImageAsync(playlistTitle)
                        .await()

                    when {
                        repImage != null ->
                            repImage.image.toBitmap()

                        itemList.isEmpty() ->
                            getAlbumPictureAsync("").await()

                        else ->
                            getAlbumPictureAsync(itemList.first().second.path)
                                .await()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        R.string.image_too_big,
                        Toast.LENGTH_LONG
                    ).show()

                    getAlbumPictureAsync("").await()
                }
            }
        )
        .transition(DrawableTransitionOptions.withCrossFade())
        .fallback(R.drawable.album_default)
        .error(R.drawable.album_default)
}