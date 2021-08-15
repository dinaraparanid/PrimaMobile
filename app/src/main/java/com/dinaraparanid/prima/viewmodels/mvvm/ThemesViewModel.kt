package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.ColorPickerDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.ThemesFragment]
 */

class ThemesViewModel(private val context: Context) :
    ViewModel() {

    /**
     * 1. Shows color picker dialog to choose primary color
     * 2. Shows dialog with night or day theme selection
     * 3. Recreates activity
     */
    @JvmName("onCustomThemeClicked")
    internal fun onCustomThemeClicked() {
        ColorPickerDialog(context, this).show(object : ColorPickerDialog.ColorPickerObserver() {
            override fun onColorPicked(color: Int) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.select_color)
                    .setSingleChoiceItems(
                        arrayOf(
                            context.resources.getString(R.string.day),
                            context.resources.getString(R.string.night)
                        ),
                        0
                    ) { _, item ->
                        val themeColors = color to item
                        Params.instance.themeColor = themeColors
                        StorageUtil(context).storeCustomThemeColors(themeColors)
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }
                    .show()
            }
        })
    }
}