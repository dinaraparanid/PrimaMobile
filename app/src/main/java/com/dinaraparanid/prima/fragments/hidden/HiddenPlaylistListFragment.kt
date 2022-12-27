package com.dinaraparanid.prima.fragments.hidden

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.mvvmp.view.dialogs.CreateHiddenPasswordDialogFragment
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.mvvmp.view.fragments.PlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewPlaylistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [TypicalViewPlaylistListFragment] for hidden playlists
 * (both albums and custom playlists)
 */

class HiddenPlaylistListFragment : TypicalViewPlaylistListFragment(), PlaylistListFragment {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_hidden, menu)
        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@HiddenPlaylistListFragment)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.change_password)
            CreateHiddenPasswordDialogFragment(
                CreateHiddenPasswordDialogFragment.Target.CREATE,
                fragmentActivity
            ).show(requireActivity().supportFragmentManager, null)

        return super.onMenuItemSelected(menuItem)
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                clear()

                addAll(
                    HiddenRepository
                        .getInstanceSynchronized()
                        .getPlaylistsAsync()
                        .await()
                        .onEach { playlist ->
                            val title = playlist.title

                            when (playlist.type) {
                                AbstractPlaylist.PlaylistType.ALBUM ->
                                    application
                                        .allTracks
                                        .firstOrNull { it.album == title }

                                AbstractPlaylist.PlaylistType.CUSTOM ->
                                    CustomPlaylistsRepository
                                        .getInstanceSynchronized()
                                        .getFirstTrackOfPlaylistAsync(title)
                                        .await()

                                AbstractPlaylist.PlaylistType.GTM ->
                                    throw IllegalArgumentException("GTM Playlist in hidden")
                            }?.let(playlist::add)
                        }
                )
            }
        }
    }
}