package com.dinaraparanid.prima.fragments.track_lists

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractCustomPlaylistTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractCustomPlaylistTrackListFragment] for user's playlists */

class CustomPlaylistTrackListFragment : AbstractCustomPlaylistTrackListFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_custom_playlist_menu_hide, menu)
                (menu.findItem(R.id.cp_search).actionView as SearchView)
                    .setOnQueryTextListener(this@CustomPlaylistTrackListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.cp_find_by -> selectSearch()
                    R.id.hide -> fragmentActivity.hidePlaylist(
                        HiddenPlaylist(
                            title = mainLabelCurText,
                            type = AbstractPlaylist.PlaylistType.CUSTOM
                        )
                    )
                }

                return true
            }
        })
    }

    /** Loads all custom playlist's tracks */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val task = CustomPlaylistsRepository
                .getInstanceSynchronized()
                .getTracksOfPlaylistAsync(playlistTitle = mainLabelCurText)

            itemList.clear()
            itemList.addAll(Params.sortedTrackList(task.await().enumerated()))
        }
    }
}