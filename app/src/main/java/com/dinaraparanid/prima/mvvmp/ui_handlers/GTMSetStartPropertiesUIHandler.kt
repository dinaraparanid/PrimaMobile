package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.mvvmp.view.dialogs.GTMSetStartPropertiesDialog
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.polymorphism.getFromIOThreadAsync
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/** [UIHandler] for [GTMSetStartPropertiesDialog] */

class GTMSetStartPropertiesUIHandler : UIHandler {
    // TODO: Normal implementation of album tracks' fetch
    private suspend inline fun getAlbumTracksAsync(
        app: MainApplication,
        albumTitle: String
    ): List<AbstractTrack> {
        val task1 = app.getAlbumTracksAsync(albumTitle)
        val task2 = app.getAlbumTracksAsync(albumTitle.lowercase())
        val task3 = app.getAlbumTracksAsync("$albumTitle ")
        val task4 = app.getAlbumTracksAsync("$albumTitle ".lowercase())
        return mutableListOf(task1, task2, task3, task4).flatMap { it.await() }
    }

    private suspend inline fun getPlaylistTracksAsync(playlistTitle: String) =
        CustomPlaylistsRepository
            .getInstanceSynchronized()
            .getTracksOfPlaylistAsync(playlistTitle)
            .await()

    private suspend inline fun getGamePlaylistAsync(
        app: MainApplication,
        playlist: AbstractPlaylist
    ) =
        when (playlist.type) {
            AbstractPlaylist.PlaylistType.ALBUM -> getAlbumTracksAsync(app, playlist.title)
            AbstractPlaylist.PlaylistType.CUSTOM -> getPlaylistTracksAsync(playlist.title)
            else -> throw IllegalArgumentException(
                "GTM Playlist should not be used with GuessTheMelodyStartParamsDialog"
            )
        }

    fun dismissAndShowError(context: Context, @StringRes message: Int, dialog: DialogInterface) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        dialog.dismiss()
    }

    private fun showErrorAndCancelPlaylistTask(
        context: Context,
        @StringRes message: Int,
        gamePlaylistTask: Job,
        dialog: DialogInterface
    ) {
        gamePlaylistTask.cancel()
        dismissAndShowError(context, message, dialog)
    }

    private inline val List<AbstractTrack>.hasEnoughTracks
        get() = size >= 4

    private fun GTMSetStartPropertiesDialog.getGTMActivityIntent(gamePlaylist: List<AbstractTrack>) =
        Intent(
            requireContext().applicationContext,
            GuessTheMelodyActivity::class.java
        ).apply {
            putExtra(
                GuessTheMelodyActivity.PLAYLIST_KEY,
                gamePlaylist
                    .shuffled()
                    .take(viewModel.presenter.gtmTracksAmount.toInt())
                    .toTypedArray()
            )

            putExtra(
                GuessTheMelodyActivity.MAX_PLAYBACK_LENGTH_KEY,
                viewModel.presenter.gtmPlaybackLen.toByte()
            )
        }

    private fun GTMSetStartPropertiesDialog.startGTMActivity(gamePlaylist: List<AbstractTrack>) =
        startActivity(getGTMActivityIntent(gamePlaylist))

    private fun GTMSetStartPropertiesDialog.checkIsInputCorrect(
        gamePlaylistTask: Deferred<List<AbstractTrack>>,
        dialog: DialogInterface
    ) {
        when {
            !viewModel.presenter.isGTMTracksEnough ->
                uiHandler.showErrorAndCancelPlaylistTask(
                    context = requireContext(),
                    message = R.string.track_number_error,
                    gamePlaylistTask = gamePlaylistTask,
                    dialog = dialog
                )

            !viewModel.presenter.isGTMPlaybackLenEnough ->
                uiHandler.showErrorAndCancelPlaylistTask(
                    context = requireContext(),
                    message = R.string.playback_time_error,
                    gamePlaylistTask = gamePlaylistTask,
                    dialog = dialog
                )

            else -> runOnUIThread {
                val gamePlaylist = gamePlaylistTask.await()

                when {
                    !gamePlaylist.hasEnoughTracks -> uiHandler.dismissAndShowError(
                        context = requireContext(),
                        message = R.string.game_playlist_small,
                        dialog = dialog
                    )

                    else -> {
                        dialog.dismiss()

                        startGTMActivity(gamePlaylist)

                        requireActivity()
                            .supportFragmentManager
                            .popBackStack()
                    }
                }
            }
        }
    }

    fun GTMSetStartPropertiesDialog.startGameOrShowError(
        playlist: AbstractPlaylist,
        dialog: DialogInterface
    ) = checkIsInputCorrect(
        gamePlaylistTask = getFromIOThreadAsync {
            uiHandler.getGamePlaylistAsync(
                app = requireActivity().application as MainApplication,
                playlist = playlist
            )
        },
        dialog
    )
}