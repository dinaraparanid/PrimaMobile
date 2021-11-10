package com.dinaraparanid.prima.utils.dialogs

import android.widget.Toast
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.ChooseFolderFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/**
 * [InputDialog] to create new folder.
 * Asks about folder's title and creates
 * new playlist or complains about error.
 */

internal class NewFolderDialog(
    fragment: WeakReference<ChooseFolderFragment>,
    path: String,
    coroutineScope: CoroutineScope
) : InputDialog(
    R.string.folder_title,
    { input ->
        Toast.makeText(
            fragment.unchecked.requireContext().applicationContext,
            when {
                File("$path/$input").mkdir() -> R.string.folder_create_success
                else -> R.string.folder_create_error
            },
            Toast.LENGTH_LONG
        ).show()

        coroutineScope.launch(Dispatchers.Main) { fragment.unchecked.updateUIAsync() }
    },
    R.string.folder_create_error
)