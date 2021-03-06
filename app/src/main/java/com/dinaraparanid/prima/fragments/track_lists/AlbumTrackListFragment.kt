package com.dinaraparanid.prima.fragments.track_lists

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractAlbumTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractAlbumTrackListFragment] for not hidden albums */

class AlbumTrackListFragment : AbstractAlbumTrackListFragment() {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_track_list_hide, menu)

        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this)

        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }

        menu.findItem(R.id.hide).setOnMenuItemClickListener {
            fragmentActivity.hidePlaylist(
                HiddenPlaylist(title = mainLabelCurText, type = AbstractPlaylist.PlaylistType.ALBUM)
            )
            true
        }
    }

    /** Loads all tracks from an album */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.apply {
                clear()

                val task1 = application.getAlbumTracksAsync(albumTitle = mainLabelCurText)
                val task2 = application.getAlbumTracksAsync(albumTitle = mainLabelCurText.lowercase())
                val task3 = application.getAlbumTracksAsync(albumTitle = "$mainLabelCurText ")
                val task4 = application.getAlbumTracksAsync(albumTitle = "$mainLabelCurText ".lowercase())

                addAll(task1.await().enumerated())
                addAll(task2.await().enumerated())
                addAll(task3.await().enumerated())
                addAll(task4.await().enumerated())
            }
        }
    }
}