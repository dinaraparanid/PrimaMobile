package com.dinaraparanid.prima.fragments.track_lists

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Track list with tracks hidden by user */

class HiddenTrackListFragment : TypicalViewTrackListFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_hidden_track_list, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }
        menu.findItem(R.id.change_password).setOnMenuItemClickListener {
            CreateHiddenPasswordDialog(CreateHiddenPasswordDialog.Target.CREATE, fragmentActivity)
                .show(requireActivity().supportFragmentManager, null)
            true
        }
    }

    /** Loads all hidden tracks */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                clear()
                addAll(application.hiddenTracks.enumerated())
            }
        }
    }
}