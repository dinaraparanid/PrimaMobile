package com.dinaraparanid.prima.utils.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.GtmStartParamsBinding
import com.dinaraparanid.prima.fragments.TrackSelectFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * Dialog to set params for game (amount of tracks and maximum playback time).
 * Tracks amount must be smaller than 9999, playback limit is 99 seconds.
 *
 * @param playlist playlist in which tracks will be guessed
 * @param fragment current [GTMPlaylistSelectFragment]
 */

class GuessTheMelodyStartParamsDialog(
    private val playlist: AbstractPlaylist,
    private val fragment: WeakReference<GTMPlaylistSelectFragment>
) : DialogFragment(), CoroutineScope by MainScope() {
    private var dialogBinding: GtmStartParamsBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogBinding = DataBindingUtil.inflate<GtmStartParamsBinding>(
            layoutInflater,
            R.layout.gtm_start_params,
            null, false
        ).apply { viewModel = ViewModel() }

        return AlertDialog.Builder(requireContext())
            .setView(dialogBinding!!.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                launch(Dispatchers.IO) {
                    val gamePlaylist = when (playlist.type) {
                        AbstractPlaylist.PlaylistType.ALBUM ->
                            (requireActivity().application as MainApplication)
                                .getAlbumTracksAsync(playlist.title)

                        AbstractPlaylist.PlaylistType.CUSTOM ->
                            CustomPlaylistsRepository.instance
                                .getTracksOfPlaylistAsync(playlist.title)

                        AbstractPlaylist.PlaylistType.GTM -> null
                    }?.await()

                    when {
                        gamePlaylist == null -> {
                            requireActivity().supportFragmentManager.beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in,
                                    R.anim.slide_out,
                                    R.anim.slide_in,
                                    R.anim.slide_out
                                )
                                .replace(
                                    R.id.fragment_container,
                                    TrackSelectFragment.newInstance(
                                        (requireActivity() as MainActivity).mainLabelCurText,
                                        TrackSelectFragment.Companion.TracksSelectionTarget.GTM
                                    )
                                )
                                .addToBackStack(null)
                                .commit()

                            dialog.dismiss()
                        }

                        gamePlaylist.isEmpty() -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.empty_game_playlist,
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                        }

                        else -> {
                            startActivity(
                                Intent(
                                    requireContext().applicationContext,
                                    GuessTheMelodyActivity::class.java
                                ).apply {
                                    putExtra(
                                        GuessTheMelodyActivity.PLAYLIST_KEY,
                                        gamePlaylist.toTypedArray()
                                    )
                                }
                            )

                            fragment.unchecked.requireActivity()
                                .supportFragmentManager
                                .popBackStack()

                            dialog.dismiss()
                        }
                    }

                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogBinding = null
    }
}