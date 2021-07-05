package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.RenamePlaylistDialog
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.TrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.updateContent
import com.google.android.material.bottomsheet.BottomSheetBehavior

class CustomPlaylistTrackListFragment : TrackListFragment() {
    companion object {
        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            _firstToHighlight: String? = null
        ): CustomPlaylistTrackListFragment = CustomPlaylistTrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putString(START_KEY, _firstToHighlight ?: NO_HIGHLIGHT)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
        updateContent(itemList)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_custom_playlist_menu, menu)

        (menu.findItem(R.id.custom_playlist_search).actionView as SearchView)
            .setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_tracks -> requireActivity().supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.fragment_container,
                    TrackSelectFragment.newInstance(
                        mainLabelCurText,
                        resources.getString(R.string.tracks),
                        itemList.toPlaylist()
                    )
                )
                .addToBackStack(null)
                .apply {
                    (requireActivity() as MainActivity).sheetBehavior.state =
                        BottomSheetBehavior.STATE_COLLAPSED
                }
                .commit()

            R.id.rename_playlist -> RenamePlaylistDialog(this)

            R.id.remove_playlist -> AreYouSureDialog(
                R.string.ays_remove_playlist,
            ) {
                CustomPlaylistsRepository.instance.run {
                    removePlaylist(CustomPlaylist.Entity(mainLabelCurText))
                    removeTracksOfPlaylist(mainLabelCurText)
                }
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun load() {
        itemList.clear()
        itemList.addAll(CustomPlaylistsRepository.instance.getTracksOfPlaylist(mainLabelCurText))
    }

    fun renameTitle(title: String) {
        mainLabelCurText = title
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
    }
}