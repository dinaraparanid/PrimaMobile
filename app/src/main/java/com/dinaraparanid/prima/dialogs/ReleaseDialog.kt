package com.dinaraparanid.prima.dialogs

import android.app.Activity
import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogNewReleaseBinding
import com.dinaraparanid.prima.databinding.DialogCurrentReleaseBinding
import com.dinaraparanid.prima.utils.web.github.Release
import com.dinaraparanid.prima.mvvmp.old_shit.ReleaseViewModel
import java.lang.ref.WeakReference

/**
 * [Dialog] that shows when the new version is released.
 * Requests user to update or just cancels it.
 *
 * @param release new [Release]
 * @param activity Main[Activity] when dialog is launched
 * @param target [Target] the dialog's creation's reason
 */

class ReleaseDialog(release: Release, activity: Activity, target: Target) : Dialog(activity) {

    /** The reason why is dialog called */
    enum class Target { NEW, CURRENT }

    init {
        setContentView(
            when (target) {
                Target.NEW -> DataBindingUtil
                    .inflate<DialogNewReleaseBinding>(
                        layoutInflater,
                        R.layout.dialog_new_release,
                        null, false
                    )
                    .apply {
                        viewModel = ReleaseViewModel(release, WeakReference(activity), target)
                        setCancelable(true)

                        updateNowButton.setOnClickListener {
                            dismiss()
                            viewModel!!.onUpdateButtonClicked()
                        }

                        updateLaterButton.setOnClickListener { dismiss() }
                        executePendingBindings()
                    }

                Target.CURRENT -> DataBindingUtil
                    .inflate<DialogCurrentReleaseBinding>(
                        layoutInflater,
                        R.layout.dialog_current_release,
                        null, false
                    )
                    .apply {
                        viewModel = ReleaseViewModel(release, WeakReference(activity), target)
                        setCancelable(true)
                        executePendingBindings()
                    }
            }.root
        )
    }
}