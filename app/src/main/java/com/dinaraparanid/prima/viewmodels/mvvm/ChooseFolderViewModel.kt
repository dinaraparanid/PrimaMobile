package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.ChooseFolderFragment]
 */

class ChooseFolderViewModel(
    private val path: String,
    private val activity: WeakReference<FoldersActivity>
) : ViewModel() {
    @JvmName("onSelectFolderButtonClicked")
    internal fun onSelectFolderButtonClicked() = activity.unchecked.run {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(FoldersActivity.FOLDER_KEY, path)
        )

        finish()
    }
}