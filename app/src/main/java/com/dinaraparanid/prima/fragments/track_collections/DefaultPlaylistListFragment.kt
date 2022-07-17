package com.dinaraparanid.prima.fragments.track_collections

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistsBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.PlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.mvvm.PlaylistListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** [AbstractPlaylistListFragment] for all user's playlists */

class DefaultPlaylistListFragment : AbstractPlaylistListFragment<FragmentCustomPlaylistsBinding>(),
    PlaylistListFragment {
    private var awaitDialog: KProgressHUD? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentCustomPlaylistsBinding>(
                inflater,
                R.layout.fragment_custom_playlists,
                container,
                false
            )
            .apply {
                viewModel = PlaylistListViewModel(WeakReference(this@DefaultPlaylistListFragment))

                mvvmViewModel = viewModel!!
                emptyTextView = playlistsEmpty

                updater = playlistSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }

                runOnUIThread {
                    awaitDialog = createAndShowAwaitDialog(requireContext(), false)

                    loadAsync().join()
                    awaitDialog?.dismiss()
                    initAdapter()

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

                        adapter = this@DefaultPlaylistListFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                        addItemDecoration(HorizontalSpaceItemDecoration(30))
                    }

                    if (application.playingBarIsVisible) up()
                }
            }

        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    /** Loads all custom playlists */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                val allPlaylistsTask = CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .getPlaylistsAndTracksAsync()

                val hiddenPlaylistsTask = HiddenRepository
                    .getInstanceSynchronized()
                    .getCustomPlaylistAsync()

                clear()
                addAll(
                    allPlaylistsTask
                        .await()
                        .map { (playlist, track) -> playlist.title to track }
                        .let { playlistsAndTracks ->
                            val hiddenPlaylists =
                                hiddenPlaylistsTask.await().map(HiddenPlaylist.Entity::title)

                            playlistsAndTracks
                                .filter { (playlist) -> playlist !in hiddenPlaylists }
                                .map { (playlist, track) ->
                                    CustomPlaylist(playlist).also { pl -> track?.let { pl.add(it) } }
                                }
                        }
                )
            }
        }
    }
}