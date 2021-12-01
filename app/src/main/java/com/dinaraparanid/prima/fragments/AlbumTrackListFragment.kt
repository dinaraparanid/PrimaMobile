package com.dinaraparanid.prima.fragments

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
import carbon.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.AlbumImage
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentPlaylistTrackListBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistTrackListViewModel
import kotlinx.coroutines.*

/**
 * [AbstractTrackListFragment] for tracks of some album
 */

class AlbumTrackListFragment :
    AbstractTrackListFragment<FragmentPlaylistTrackListBinding>(),
    ChangeImageFragment {
    override var binding: FragmentPlaylistTrackListBinding? = null
    override var updater: SwipeRefreshLayout? = null
    override var amountOfTracks: TextView? = null
    override var trackOrderTitle: TextView? = null
    override var emptyTextView: android.widget.TextView? = null

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
            viewModel = PlaylistTrackListViewModel(this@AlbumTrackListFragment)

            updater = playlistTrackSwipeRefreshLayout.apply {
                setColorSchemeColors(Params.instance.primaryColor)
                setOnRefreshListener {
                    try {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIAsync(itemList)
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
                    val progress = createAndShowAwaitDialog(requireContext(), false)

                    task.join()
                    progress.dismiss()

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
                                            .instance
                                            .getAlbumWithImageAsync(mainLabelCurText)
                                            .await()

                                        when {
                                            repImage != null -> repImage.image.toBitmap()

                                            itemList.isEmpty() -> getAlbumPictureAsync(
                                                "",
                                                true
                                            ).await()

                                            else -> getAlbumPictureAsync(
                                                itemList.first().path,
                                                Params.instance.isPlaylistsImagesShown
                                            ).await()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.image_too_big,
                                            Toast.LENGTH_LONG
                                        ).show()

                                        getAlbumPictureAsync("", true).await()
                                    }
                                }
                            )
                            .skipMemoryCache(true)
                            .override(playlistTracksImage.width, playlistTracksImage.height)
                            .into(playlistTracksImage)
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

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.apply {
                clear()
                addAll(application.getAlbumTracksAsync(mainLabelCurText).await())
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
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        val albumImage = AlbumImage(
                            mainLabelCurText,
                            resource.toByteArray()
                        )

                        val rep = ImageRepository.instance

                        runOnIOThread {
                            rep.removeAlbumWithImageAsync(mainLabelCurText).join()

                            try {
                                rep.addAlbumWithImageAsync(albumImage).join()

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
                                rep.removeAlbumWithImageAsync(mainLabelCurText)

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