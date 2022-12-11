package com.dinaraparanid.prima.fragments.hidden

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.view.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Track list with tracks hidden by user */

class HiddenTrackListFragment : TypicalViewTrackListFragment() {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_hidden_track_list, menu)
        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@HiddenTrackListFragment)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.find_by -> selectSearch()
            R.id.change_password -> CreateHiddenPasswordDialog(
                CreateHiddenPasswordDialog.Target.CREATE,
                fragmentActivity
            ).show(requireActivity().supportFragmentManager, null)
        }

        return super.onMenuItemSelected(menuItem)
    }

    /** Loads all hidden tracks */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                clear()
                addAll(Params.sortedTrackList(application.hiddenTracks.enumerated()))
            }
        }
    }
}