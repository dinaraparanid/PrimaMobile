package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ConstraintLayout
import carbon.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.covers.AlbumCover
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databinding.FragmentPlaylistTrackListBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistTrackListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** [AbstractAlbumTrackListFragment] for tracks of some album */

abstract class AbstractAlbumTrackListFragment :
    TrackCollectionTrackListFragment<FragmentPlaylistTrackListBinding>(),
    ChangeImageFragment {
    final override var binding: FragmentPlaylistTrackListBinding? = null
    final override var updater: SwipeRefreshLayout? = null
    final override var amountOfTracks: TextView? = null
    final override var trackOrderTitle: TextView? = null
    final override var emptyTextView: android.widget.TextView? = null
    final override val addPlaylistToFavouritesButton get() = binding!!.addPlaylistToFavouritesButton
    private var awaitDialog: KProgressHUD? = null

    /** Asynchronously loads covers */
    private suspend fun loadImages() {
        binding?.playlistTracksImageLayout?.run {
            initGlideAsync()
                .override(
                    binding!!.playlistTracksImage.width,
                    binding!!.playlistTracksImage.height
                ).into(binding!!.playlistTracksImage)

            if (!Params.getInstanceSynchronized().isCustomTheme) {
                val imageLayout = binding!!.playlistTracksImageLayout
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
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentPlaylistTrackListBinding>(
            inflater,
            R.layout.fragment_playlist_track_list,
            container,
            false
        ).apply {
            viewModel = PlaylistTrackListViewModel(
                playlistTitle = mainLabelCurText,
                playlistType = AbstractPlaylist.PlaylistType.ALBUM.ordinal,
                fragment = this@AbstractAlbumTrackListFragment
            )

            runOnUIThread {
                addPlaylistToFavouritesButton.setImageResource(
                    when {
                        // IDK why, but it doesn't think that it's bool...
                        viewModel!!
                            .isPlaylistLikedAsync()
                            .await() as Boolean -> R.drawable.heart_like_white

                        else -> R.drawable.heart_white
                    }
                )
            }

            updater = playlistTrackSwipeRefreshLayout.apply {
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

            try {
                runOnUIThread {
                    val task = loadAsync()
                    awaitDialog = createAndShowAwaitDialog(requireContext(), false)

                    task.join()
                    awaitDialog?.dismiss()
                    initAdapter()

                    emptyTextView = playlistTrackListEmpty
                    setEmptyTextViewVisibility(itemList)
                    itemListSearch.addAll(itemList)
                    adapter.setCurrentList(itemList)
                    loadImages()

                    amountOfTracks = amountOfTracksPlaylist.apply {
                        val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                        text = txt
                    }

                    trackOrderTitle = playlistTrackOrderTitle

                    recyclerView = playlistTrackRecyclerView.apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@AbstractAlbumTrackListFragment.adapter.apply {
                            stateRestorationPolicy =
                                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                    }

                    updateOrderTitle()
                    if (application.playingBarIsVisible) up()
                }
            } catch (ignored: Exception) {
                // permissions not given
            }
        }

        return binding!!.root
    }

    /** Frees UI */
    final override fun onStop() {
        binding?.playlistTracksImage?.let(Glide.with(this)::clear)
        super.onStop()
    }

    final override fun onResume() {
        super.onResume()
        runOnUIThread { loadImages() }
    }

    final override fun onDestroyView() {
        binding?.playlistTracksImage?.let(Glide.with(this)::clear)
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    /**
     * Sets new album's cover by given [image]'s uri
     * and updates [CoversRepository] database
     */

    final override suspend fun setUserImageAsync(image: Uri) = runOnUIThread {
        Glide.with(this@AbstractAlbumTrackListFragment)
            .asBitmap()
            .load(image)
            .skipMemoryCache(true)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val albumImage = AlbumCover(
                            mainLabelCurText,
                            resource.toByteArray()
                        )

                        runOnIOThread {
                            CoversRepository
                                .getInstanceSynchronized()
                                .removeAlbumWithCoverAsync(title = mainLabelCurText)
                                .join()

                            try {
                                CoversRepository
                                    .getInstanceSynchronized()
                                    .addAlbumsWithCoverAsync(albumImage)
                                    .join()

                                launch(Dispatchers.Main) {
                                    Glide.with(this@AbstractAlbumTrackListFragment)
                                        .load(image)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(
                                            binding!!.playlistTracksImage.width,
                                            binding!!.playlistTracksImage.height
                                        )
                                        .into(binding!!.playlistTracksImage)

                                    if (!Params.getInstanceSynchronized().isCustomTheme)
                                        Glide.with(this@AbstractAlbumTrackListFragment)
                                            .load(image)
                                            .skipMemoryCache(true)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .override(
                                                binding!!.playlistTracksImageLayout.width,
                                                binding!!.playlistTracksImageLayout.height
                                            )
                                            .transform(BlurTransformation(15, 5))
                                            .into(
                                                object : CustomViewTarget<ConstraintLayout, Drawable>(
                                                    binding!!.playlistTracksImageLayout
                                                ) {
                                                    override fun onLoadFailed(errorDrawable: Drawable?) = Unit
                                                    override fun onResourceCleared(placeholder: Drawable?) = Unit

                                                    override fun onResourceReady(
                                                        resource: Drawable,
                                                        transition: Transition<in Drawable>?
                                                    ) {
                                                        binding!!
                                                            .playlistTracksImageLayout
                                                            .background = resource
                                                    }
                                                }
                                            )
                                }
                            } catch (e: Exception) {
                                CoversRepository
                                    .getInstanceSynchronized()
                                    .removeAlbumWithCoverAsync(title = mainLabelCurText)

                                runOnUIThread {
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
            )
    }

    /** Prepares Glide to load album's cover */
    private suspend fun initGlideAsync() = Glide.with(this)
        .load(
            application.run {
                try {
                    val repImage = CoversRepository
                        .getInstanceSynchronized()
                        .getAlbumWithCoverAsync(title = mainLabelCurText)
                        .await()

                    when {
                        repImage != null -> repImage.image.toBitmap()

                        itemList.isEmpty() -> getAlbumPictureAsync("")
                            .await()

                        else -> getAlbumPictureAsync(itemList.first().second.path)
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
        .skipMemoryCache(true)
        .transition(DrawableTransitionOptions.withCrossFade())
}