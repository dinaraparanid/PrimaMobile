package com.dinaraparanid.prima.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.FragmentPlaylistsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * [AbstractPlaylistListFragment] for all user's playlists
 */

class PlaylistListFragment : AbstractPlaylistListFragment<FragmentPlaylistsBinding>() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        titleDefault = resources.getString(R.string.playlists)

        binding = DataBindingUtil
            .inflate<FragmentPlaylistsBinding>(
                inflater,
                R.layout.fragment_playlists,
                container,
                false
            )
            .apply {
                viewModel = PlaylistListViewModel(WeakReference(this@PlaylistListFragment))

                mvvmViewModel = viewModel!!
                emptyTextView = playlistsEmpty

                updater = playlistSwipeRefreshLayout.apply {
                    setOnRefreshListener {
                        setColorSchemeColors(Params.instance.primaryColor)
                        runOnUIThread {
                            loadAsync().join()
                            updateUI(isLocking = true)
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

                        adapter = this@PlaylistListFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        addItemDecoration(HorizontalSpaceItemDecoration(30))
                    }

                    if (application.playingBarIsVisible) up()
                }
            }

        return binding!!.root
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                val task = CustomPlaylistsRepository.instance.getPlaylistsWithTracksAsync()
                clear()
                addAll(
                    task.await().map { (p, t) ->
                        CustomPlaylist(p).apply {
                            t.takeIf { it.isNotEmpty() }?.let { add(t.first()) }
                        }
                    }
                )
            }
        }
    }
}