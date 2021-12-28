package com.dinaraparanid.prima.utils.dialogs

import android.app.Activity
import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogNewReleaseBinding
import com.dinaraparanid.prima.utils.web.github.Release
import com.dinaraparanid.prima.viewmodels.mvvm.NewReleaseViewModel
import java.lang.ref.WeakReference

/**
 * [Dialog] that shows when the new version is released.
 * Requests user to update or just cancels it.
 *
 * @param release new [Release]
 * @param activity Main[Activity] when dialog is launched
 */

class NewReleaseDialog(release: Release, activity: Activity) : Dialog(activity) {
    init {
        setContentView(
            DataBindingUtil
                .inflate<DialogNewReleaseBinding>(
                    layoutInflater,
                    R.layout.dialog_new_release,
                    null, false
                )
                .apply {
                    viewModel = NewReleaseViewModel(release, WeakReference(activity))
                    setTitle(viewModel!!.version)
                    setCancelable(true)

                    updateNowButton.setOnClickListener {
                        dismiss()
                        viewModel!!.onUpdateButtonClicked()
                    }

                    updateLaterButton.setOnClickListener { dismiss() }
                    executePendingBindings()
                }
                .root
        )
    }
}