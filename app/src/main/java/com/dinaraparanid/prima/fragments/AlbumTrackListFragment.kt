package com.dinaraparanid.prima.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.AlbumImage
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentPlaylistTrackListBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
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
    override lateinit var amountOfTracks: TextView
    override lateinit var trackOrderTitle: TextView
    override lateinit var emptyTextView: android.widget.TextView

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
            viewModel = PlaylistTrackListViewModel(this@AlbumTrackListFragment, requireActivity())

            updater = playlistTrackSwipeRefreshLayout.apply {
                setColorSchemeColors(Params.instance.primaryColor)
                setOnRefreshListener {
                    try {
                        this@AlbumTrackListFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                            loadAsync().join()
                            updateUI(itemList)
                            isRefreshing = false
                        }
                    } catch (ignored: Exception) {
                        // permissions not given
                    }
                }
            }

            try {
                this@AlbumTrackListFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                    val task = loadAsync()
                    val progress = createAndShowAwaitDialog(requireContext(), false)

                    task.join()
                    progress.dismiss()

                    emptyTextView = playlistTrackListEmpty
                    setEmptyTextViewVisibility(itemList)
                    itemListSearch.addAll(itemList)

                    adapter = TrackAdapter(itemList).apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }

                    playlistTracksImageLayout.run {
                        Glide.with(this@AlbumTrackListFragment)
                            .load(
                                (requireActivity().application as MainApplication).run {
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
                        adapter = this@AlbumTrackListFragment.adapter?.apply {
                            stateRestorationPolicy =
                                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                    }

                    updateOrderTitle()
                    if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
                }
            } catch (ignored: Exception) {
                // permissions not given
            }
        }

        (requireActivity() as MainActivity).binding!!.mainLabel.text = mainLabelCurText
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
            itemList.run {
                clear()
                addAll(
                    try {
                        val selection = "${MediaStore.Audio.Media.ALBUM} = ?"

                        val order = "${
                            when (Params.instance.tracksOrder.first) {
                                Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                                Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                                Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                                Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                            }
                        } ${if (Params.instance.tracksOrder.second) "ASC" else "DESC"}"

                        val trackList = mutableListOf<Track>()

                        val projection = mutableListOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DATE_ADDED
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

                        requireActivity().contentResolver.query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projection.toTypedArray(),
                            selection,
                            arrayOf(mainLabelCurText),
                            order
                        ).use { cursor ->
                            if (cursor != null)
                                (requireActivity().application as MainApplication)
                                    .addTracksFromStorage(cursor, trackList)
                        }

                        trackList.distinctBy(Track::path).toPlaylist()
                    } catch (e: Exception) {
                        // Permission to storage not given
                        DefaultPlaylist()
                    }
                )
                Unit
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

                        viewModel.viewModelScope.launch(Dispatchers.IO) {
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
            )
    }
}