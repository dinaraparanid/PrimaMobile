package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import com.dinaraparanid.prima.databases.entities.covers.PlaylistCover
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.dialogs.QuestionDialog
import com.dinaraparanid.prima.dialogs.RenamePlaylistDialog
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.extensions.tracks
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.mvvmp.old_shit.CustomPlaylistTrackListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** [AbstractTrackListFragment] for user's playlists */

abstract class AbstractCustomPlaylistTrackListFragment :
    TrackCollectionTrackListFragment<FragmentCustomPlaylistTrackListBinding>(),
    ChangeImageFragment {
    private var playlistId = 0L
    private var awaitDialog: KProgressHUD? = null
    internal val playlistTitle: String by lazy { mainLabelCurText.get() }

    final override var binding: FragmentCustomPlaylistTrackListBinding? = null
    final override var updater: SwipeRefreshLayout? = null
    final override var emptyTextView: TextView? = null
    final override var amountOfTracks: carbon.widget.TextView? = null
    final override var trackOrderTitle: carbon.widget.TextView? = null
    final override val addPlaylistToFavouritesButton get() = binding!!.addPlaylistToFavouritesButton

    internal companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelCurText main label text for current fragment
         * @param playlistId id of playlist
         * @param clazz Kotlin class of fragment
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelCurText: String,
            playlistId: Long,
            clazz: KClass<out AbstractCustomPlaylistTrackListFragment>
        ) = clazz.constructors.first().call().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.rename_playlist -> RenamePlaylistDialog(this)
                .show(requireActivity().supportFragmentManager, null)

            R.id.remove_playlist -> QuestionDialog(
                R.string.ays_remove_playlist,
            ) {
                runOnIOThread {
                    StatisticsRepository
                        .getInstanceSynchronized()
                        .removeCustomPlaylistAsync(title = mainLabelCurText.get())

                    CoversRepository
                        .getInstanceSynchronized()
                        .removePlaylistWithImageAsync(title = mainLabelCurText.get())

                    FavouriteRepository
                        .getInstanceSynchronized()
                        .getPlaylistAsync(
                            title = mainLabelCurText.get(),
                            type = AbstractPlaylist.PlaylistType.CUSTOM.ordinal
                        )
                        .await()
                        ?.let {
                            FavouriteRepository
                                .getInstanceSynchronized()
                                .removePlaylistsAsync(it)
                        }
                }

                CustomPlaylistsRepository.getInstanceSynchronized().run {
                    runOnIOThread {
                        removePlaylistAsync(title = mainLabelCurText.get())
                        removeTracksOfPlaylistAsync(title = mainLabelCurText.get())
                    }
                }

                requireActivity().supportFragmentManager.popBackStack()
            }.show(requireActivity().supportFragmentManager, null)
        }

        return super.onMenuItemSelected(menuItem)
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
    }

    /* Loads covers for playlists */
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

    final override fun onCreateView(
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
                    fragment = this@AbstractCustomPlaylistTrackListFragment,
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
                                loadAsync().join()
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

                        task.join()
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
                            adapter = this@AbstractCustomPlaylistTrackListFragment.adapter
                            addItemDecoration(VerticalSpaceItemDecoration(30))

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
                                Params.getInstanceSynchronized().areDividersShown
                            ) addItemDecoration(
                                DividerItemDecoration(requireContext(), Divider.instance)
                            )
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

        fragmentActivity.mainLabelCurText = mainLabelCurText.get()
        return binding!!.root
    }

    /** Frees UI */
    final override fun onStop() {
        binding?.customPlaylistTracksImage?.let(Glide.with(this)::clear)
        super.onStop()
    }

    /** Refreshes UI */
    final override fun onResume() {
        super.onResume()
        runOnUIThread { loadImages() }
    }

    /** Frees dialogs */
    final override fun onDestroyView() {
        binding?.customPlaylistTracksImage?.let(Glide.with(this)::clear)
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    /**
     * Sets new cover by given [image] URI
     * and updates [CoversRepository] database
     */

    final override suspend fun setUserImageAsync(image: Uri) = runOnUIThread {
        Glide.with(this@AbstractCustomPlaylistTrackListFragment)
            .asBitmap()
            .load(image)
            .skipMemoryCache(true)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val playlistImage = PlaylistCover(
                            playlistTitle,
                            resource.toByteArray()
                        )

                        runOnIOThread {
                            CoversRepository
                                .getInstanceSynchronized()
                                .removePlaylistWithImageAsync(playlistTitle)
                                .join()

                            try {
                                CoversRepository
                                    .getInstanceSynchronized()
                                    .addPlaylistsWithImageAsync(playlistImage)

                                launch(Dispatchers.Main) {
                                    Glide.with(this@AbstractCustomPlaylistTrackListFragment)
                                        .load(image)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(
                                            binding!!.customPlaylistTracksImage.width,
                                            binding!!.customPlaylistTracksImage.height
                                        )
                                        .into(binding!!.customPlaylistTracksImage)

                                    if (!Params.getInstanceSynchronized().isCustomTheme)
                                        Glide.with(this@AbstractCustomPlaylistTrackListFragment)
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
                                CoversRepository
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
        mainLabelCurText.set(title)
        fragmentActivity.mainLabelCurText = mainLabelCurText.get()
    }

    /** Prepares Glide to load playlist's cover */
    private suspend fun initGlideAsync() = Glide.with(this)
        .load(
            application.run {
                try {
                    val repImage = CoversRepository
                        .getInstanceSynchronized()
                        .getPlaylistWithCoverAsync(playlistTitle)
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