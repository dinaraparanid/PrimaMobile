package com.dinaraparanid.prima.fragments.track_lists

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

/** [AbstractCustomPlaylistTrackListFragment] for user's playlists */

class CustomPlaylistTrackListFragment : AbstractCustomPlaylistTrackListFragment() {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_custom_playlist_menu_hide, menu)

        (menu.findItem(R.id.cp_search).actionView as SearchView)
            .setOnQueryTextListener(this@CustomPlaylistTrackListFragment)

        menu.findItem(R.id.cp_find_by).setOnMenuItemClickListener { selectSearch() }

        menu.findItem(R.id.hide).setOnMenuItemClickListener {
            fragmentActivity.hidePlaylist(
                HiddenPlaylist(
                    title = mainLabelCurText,
                    type = AbstractPlaylist.PlaylistType.CUSTOM
                )
            )
            true
        }
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