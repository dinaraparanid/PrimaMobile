package com.dinaraparanid.prima.fragments.hidden

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

class HiddenCustomPlaylistTrackListFragment : AbstractCustomPlaylistTrackListFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_custom_playlist_menu_show, menu)
                (menu.findItem(R.id.cp_search).actionView as SearchView)
                    .setOnQueryTextListener(this@HiddenCustomPlaylistTrackListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.cp_find_by -> selectSearch()
                    R.id.show -> fragmentActivity.removePlaylistFromHidden(
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

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()
            itemList.addAll(
                Params.sortedTrackList(
                    CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getTracksOfPlaylistAsync(playlistTitle = mainLabelCurText)
                        .await()
                        .enumerated()
                )
            )
        }
    }
}