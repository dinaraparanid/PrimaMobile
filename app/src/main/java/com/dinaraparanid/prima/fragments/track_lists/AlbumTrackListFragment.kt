package com.dinaraparanid.prima.fragments.track_lists

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
import com.dinaraparanid.prima.databases.entities.images.AlbumImage
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentPlaylistTrackListBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistTrackListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.*

/** [TrackCollectionTrackListFragment] for tracks of some album */

class AlbumTrackListFragment :
    TrackCollectionTrackListFragment<FragmentPlaylistTrackListBinding>(),
    ChangeImageFragment {
    override var binding: FragmentPlaylistTrackListBinding? = null
    override var updater: SwipeRefreshLayout? = null
    override var amountOfTracks: TextView? = null
    override var trackOrderTitle: TextView? = null
    override var emptyTextView: android.widget.TextView? = null
    override val addPlaylistToFavouritesButton get() = binding!!.addPlaylistToFavouritesButton
    private var awaitDialog: KProgressHUD? = null

    override fun onCreateView(
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
                fragment = this@AlbumTrackListFragment
            )

            runOnUIThread {
                addPlaylistToFavouritesButton.setImageResource(
                    when {
                        // IDK why, but it doesn't think that it's bool...
                        viewModel!!.isPlaylistLikedAsync().await() as Boolean -> R.drawable.heart_like_white
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

                    playlistTracksImageLayout.run {
                        Glide.with(this@AlbumTrackListFragment)
                            .load(
                                application.run {
                                    try {
                                        val repImage = ImageRepository
                                            .getInstanceSynchronized()
                                            .getAlbumWithImageAsync(title = mainLabelCurText)
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
                            .run {
                                override(playlistTracksImage.width, playlistTracksImage.height)
                                    .into(playlistTracksImage)

                                val imageLayout = playlistTracksImageLayout
                                override(imageLayout.width, imageLayout.height)
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

                    amountOfTracks = amountOfTracksPlaylist.apply {
                        val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                        text = txt
                    }

                    trackOrderTitle = playlistTrackOrderTitle

                    recyclerView = playlistTrackRecyclerView.apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@AlbumTrackListFragment.adapter.apply {
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_track_search, menu)

        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this)

        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.apply {
                clear()
                addAll(application.getAlbumTracksAsync(mainLabelCurText).await().enumerated())
            }
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
                        val albumImage = AlbumImage(
                            mainLabelCurText,
                            resource.toByteArray()
                        )

                        runOnIOThread {
                            ImageRepository
                                .getInstanceSynchronized()
                                .removeAlbumWithImageAsync(title = mainLabelCurText)
                                .join()

                            try {
                                ImageRepository
                                    .getInstanceSynchronized()
                                    .addAlbumWithImageAsync(albumImage)
                                    .join()

                                launch(Dispatchers.Main) {
                                    Glide.with(this@AlbumTrackListFragment)
                                        .load(image)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(
                                            binding!!.playlistTracksImage.width,
                                            binding!!.playlistTracksImage.height
                                        )
                                        .into(binding!!.playlistTracksImage)
                                }
                            } catch (e: Exception) {
                                ImageRepository
                                    .getInstanceSynchronized()
                                    .removeAlbumWithImageAsync(title = mainLabelCurText)

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
}