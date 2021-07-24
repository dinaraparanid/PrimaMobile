package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.RenamePlaylistDialog
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.TrackListFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*

/**
 * [TrackListFragment] for user's playlists
 */

class CustomPlaylistTrackListFragment : TrackListFragment() {
    private var playlistId = 0L
    val mainLabel: String by lazy { mainLabelCurText }

    companion object {
        private const val PLAYLIST_ID_KEY = "playlist_id"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param playlistId id of playlist
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            playlistId: Long,
        ): CustomPlaylistTrackListFragment = CustomPlaylistTrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putLong(PLAYLIST_ID_KEY, playlistId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(PLAYLIST_ID_KEY)
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
                        playlistId,
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
                .show(requireActivity().supportFragmentManager, null)

            R.id.remove_playlist -> AreYouSureDialog(
                R.string.ays_remove_playlist,
            ) {
                CustomPlaylistsRepository.instance.run {
                    removePlaylist(mainLabelCurText)
                    removeTracksOfPlaylist(mainLabelCurText)
                }
                requireActivity().supportFragmentManager.popBackStack()
            }.show(requireActivity().supportFragmentManager, null)
        }

        return super.onOptionsItemSelected(item)
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            val task = CustomPlaylistsRepository.instance
                .getTracksOfPlaylistAsync(mainLabelCurText)

            itemList.clear()
            itemList.addAll(task.await())
            Unit
        }
    }

    /**
     * Renames main label when playlist is rename
     * @param title new playlist's title
     */

    fun renameTitle(title: String) {
        mainLabelCurText = title
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
    }
}