package com.dinaraparanid.prima.utils.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.polymorphism.ContentUpdatable
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.dinaraparanid.prima.utils.polymorphism.updateContent

internal class NewPlaylistDialog<T, C>(private val fragment: T) : DialogFragment()
        where T : Loader<C>, T : ContentUpdatable<C> {
    private lateinit var input: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.playlist_title)
            .apply { input = EditText(requireContext()) }
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    CustomPlaylistsRepository
                        .instance
                        .addPlaylist(CustomPlaylist.Entity(input.text.toString()))

                    fragment.load()
                    fragment.updateContent(fragment.loaderContent)
                } catch (e: Exception) {
                    dialog!!.cancel()
                    PlaylistExistsDialog().show(parentFragmentManager, null)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog!!.cancel() }
            .create()
}