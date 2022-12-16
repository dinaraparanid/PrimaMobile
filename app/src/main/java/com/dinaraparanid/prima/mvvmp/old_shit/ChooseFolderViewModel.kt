package com.dinaraparanid.prima.mvvmp.old_shit

import android.app.Activity
import android.content.Intent
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.fragments.ChooseFolderFragment
import com.dinaraparanid.prima.mvvmp.view.dialogs.NewFolderDialogFragment
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.extensions.unchecked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.lang.ref.WeakReference

/** MVVM View Model for [ChooseFolderFragment] */

class ChooseFolderViewModel(
    private val path: String,
    private val fragment: WeakReference<ChooseFolderFragment>
) : BasePresenter(), CoroutineScope by MainScope() {
    @JvmName("onSelectFolderButtonClicked")
    internal fun onSelectFolderButtonClicked() = fragment.unchecked.requireActivity().run {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(FoldersActivity.FOLDER_KEY, path)
        )

        finishAndRemoveTask()
    }

    @JvmName("onAddFolderButtonClicked")
    internal fun onAddFolderButtonClicked() =
        NewFolderDialogFragment(WeakReference(fragment.unchecked), path)
            .show(fragment.unchecked.requireActivity().supportFragmentManager, null)
}