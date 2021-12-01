package com.dinaraparanid.prima.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databinding.FragmentAlbumsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [AbstractPlaylistListFragment] for all albums
 */

class AlbumListFragment : AbstractPlaylistListFragment<FragmentAlbumsBinding>() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        titleDefault = resources.getString(R.string.albums)

        binding = DataBindingUtil
            .inflate<FragmentAlbumsBinding>(
                inflater,
                R.layout.fragment_albums,
                container,
                false
            )
            .apply {
                viewModel = ViewModel()

                mvvmViewModel = viewModel!!
                emptyTextView = playlistsEmpty

                updater = playlistSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIAsync()
                            isRefreshing = false
                        }
                    }
                }

                runOnUIThread {
                    val progress = createAndShowAwaitDialog(requireContext(), false)

                    loadAsync().join()
                    progress.dismiss()

                    itemListSearch.addAll(itemList)
                    adapter.setCurrentList(itemList)
                    setEmptyTextViewVisibility(itemList)

                    recyclerView = playlistRecyclerView.apply {
                        layoutManager = when (resources.configuration.orientation) {
                            Configuration.ORIENTATION_PORTRAIT ->
                                when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                    Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                        GridLayoutManager(context, 2)

                                    Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                        GridLayoutManager(context, 3)

                                    else -> GridLayoutManager(context, 2)
                                }

                            else -> when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                    GridLayoutManager(context, 3)

                                Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                    GridLayoutManager(context, 4)

                                else -> GridLayoutManager(context, 3)
                            }
                        }

                        adapter = this@AlbumListFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        addItemDecoration(HorizontalSpaceItemDecoration(30))
                    }

                    if (application.playingBarIsVisible) up()
                }
            }

        return binding!!.root
    }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM),
                    null,
                    null,
                    MediaStore.Audio.Media.ALBUM + " ASC"
                ).use { cursor ->
                    itemList.clear()

                    if (cursor != null) {
                        val playlistList = mutableListOf<AbstractPlaylist>()

                        while (cursor.moveToNext()) {
                            val albumTitle = cursor.getString(0)

                            application.allTracks
                                .firstOrNull { it.playlist == albumTitle }
                                ?.let { track ->
                                    playlistList.add(
                                        DefaultPlaylist(
                                            albumTitle,
                                            AbstractPlaylist.PlaylistType.ALBUM,
                                            track
                                        )
                                    )
                                }
                        }

                        itemList.addAll(playlistList.distinctBy(AbstractPlaylist::title))
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }
}