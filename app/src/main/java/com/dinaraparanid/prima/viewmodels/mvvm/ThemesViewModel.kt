package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.AlertDialog
import android.content.Intent
import android.provider.MediaStore
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.dialogs.ColorPickerDialog
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.drawables.FontDivider
import com.dinaraparanid.prima.utils.drawables.Marker
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.fragments.ChangeImageFragment
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.main_menu.settings.ThemesFragment]
 */

class ThemesViewModel(private val activity: WeakReference<MainActivity>) : ViewModel() {

    /**
     * 1. Shows color picker dialog to choose primary color
     * 2. Shows dialog with night or day theme selection
     * 3. Recreates activity
     */

    @JvmName("onCustomThemeClicked")
    internal fun onCustomThemeClicked() = ColorPickerDialog(WeakReference(activity.unchecked), this)
        .show(object : ColorPickerDialog.ColorPickerObserver() {
            override fun onColorPicked(color: Int) {
                AlertDialog.Builder(activity.unchecked)
                    .setTitle(R.string.select_color)
                    .setSingleChoiceItems(
                        arrayOf(
                            activity.unchecked.resources.getString(R.string.day),
                            activity.unchecked.resources.getString(R.string.night)
                        ),
                        -1
                    ) { dialog, item ->
                        val themeColors = color to item
                        params.themeColor = themeColors
                        StorageUtil.instance.storeCustomThemeColors(themeColors)

                        Divider.update()
                        FontDivider.update()
                        Marker.update()
                        dialog.dismiss()

                        activity.unchecked.let {
                            it.finishWork()
                            it.startActivity(
                                Intent(
                                    params.application.unchecked,
                                    MainActivity::class.java,
                                )
                            )
                        }
                    }
                    .show()
            }
        })

    /**
     * Sends intent to set picture from user's gallery
     * as app's background image
     */

    @JvmName("onSetBackgroundPictureClicked")
    internal fun onSetBackgroundPictureClicked() = AlertDialog.Builder(activity.unchecked)
        .setSingleChoiceItems(
            arrayOf(
                activity.unchecked.resources.getString(R.string.set_background_picture),
                activity.unchecked.resources.getString(R.string.remove_background_picture)
            ),
            -1
        ) { dialog, item ->

            when (item) {
                0 -> activity.unchecked.startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    ), ChangeImageFragment.PICK_IMAGE
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