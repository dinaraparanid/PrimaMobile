package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.fragments.ChooseFolderFragment
import com.dinaraparanid.prima.utils.dialogs.NewFolderDialog
import com.dinaraparanid.prima.utils.extensions.unchecked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.lang.ref.WeakReference

/**
 * MVVM View Model for [ChooseFolderFragment]
 */

class ChooseFolderViewModel(
    private val path: String,
    private val fragment: WeakReference<ChooseFolderFragment>
) : ViewModel(), CoroutineScope by MainScope() {
    @JvmName("onSelectFolderButtonClicked")
    internal fun onSelectFolderButtonClicked() = fragment.unchecked.requireActivity().run {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(FoldersActivity.FOLDER_KEY, path)
        )

        finish()
    }

    @JvmName("onAddFolderButtonClicked")
    internal fun onAddFolderButtonClicked() =
        NewFolderDialog(WeakReference(fragment.unchecked), path, this)
            .show(fragment.unchecked.requireActivity().supportFragmentManager, null)
}