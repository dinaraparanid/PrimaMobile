package com.dinaraparanid.prima.mvvmp.old_shit

import android.app.AlertDialog
import android.content.Intent
import android.provider.MediaStore
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.view.dialogs.ColorPickerDialog
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.main_menu.settings.ThemesFragment]
 */

class ThemesViewModel(private val activity: WeakReference<MainActivity>) : BasePresenter() {

    /**
     * 1. Shows color picker dialog to choose primary color
     * 2. Shows dialog with night or day theme selection
     * 3. Recreates activity
     */

    @JvmName("onCustomThemeClicked")
    internal fun onCustomThemeClicked() = ColorPickerDialog(WeakReference(activity.unchecked), this)
        .show()

    /**
     * Sends intent to set picture from user's gallery
     * as app's background image
     */

    @JvmName("onSetBackgroundPictureClicked")
    internal fun onSetBackgroundPictureClicked(): AlertDialog =
        AlertDialog.Builder(activity.unchecked)
            .setSingleChoiceItems(
                arrayOf(
                    activity.unchecked.resources.getString(R.string.set_background_picture),
                    activity.unchecked.resources.getString(R.string.remove_background_picture)
                ),
                -1
            ) { dialog, item ->
                when (item) {
                    0 -> activity.unchecked.pickImageIntentResultListener.launch(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                    )

                    else -> {
                        StorageUtil.instance.clearBackgroundImage()
                        params.backgroundImage = null
                        activity.unchecked.updateBackgroundViewOnRemoveUserImage()
                        notifyPropertyChanged(BR._all)
                    }
                }

                dialog.dismiss()
            }
            .show()
}