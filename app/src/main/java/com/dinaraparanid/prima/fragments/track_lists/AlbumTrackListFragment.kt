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
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractAlbumTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractAlbumTrackListFragment] for not hidden albums */

class AlbumTrackListFragment : AbstractAlbumTrackListFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_track_list_hide, menu)
                (menu.findItem(R.id.find).actionView as SearchView)
                    .setOnQueryTextListener(this@AlbumTrackListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.find_by -> selectSearch()
                    R.id.hide -> fragmentActivity.hidePlaylist(
                        HiddenPlaylist(
                            title = mainLabelCurText,
                            type = AbstractPlaylist.PlaylistType.ALBUM
                        )
                    )
                }

                return true
            }
        })
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