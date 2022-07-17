package com.dinaraparanid.prima.fragments.hidden

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
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
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_custom_playlist_menu_show, menu)

        (menu.findItem(R.id.cp_search).actionView as SearchView)
            .setOnQueryTextListener(this)

        menu.findItem(R.id.cp_find_by).setOnMenuItemClickListener { selectSearch() }

        menu.findItem(R.id.show).setOnMenuItemClickListener {
            fragmentActivity.removePlaylistFromHidden(
                HiddenPlaylist(
                    title = mainLabelCurText,
                    type = AbstractPlaylist.PlaylistType.CUSTOM
                )
            )
            true
        }
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