package com.dinaraparanid.prima.viewmodels.mvvm

import android.content.Intent
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import top.defaults.colorpicker.ColorPickerPopup

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.ThemesFragment]
 */

class ThemesViewModel(private val themesLayout: LinearLayout) : ViewModel() {

    /**
     * 1. Shows color picker dialog to choose primary color
     * 2. Shows dialog with night or day theme selection
     * 3. Recreates activity
     */
    @JvmName("onCustomThemeClicked")
    internal fun onCustomThemeClicked() {
        ColorPickerPopup.Builder(params.application)
            .initialColor(Params.instance.theme.rgb)
            .enableBrightness(true)
            .enableAlpha(true)
            .okTitle(params.application.resources.getString(R.string.select_color))
            .cancelTitle(params.application.resources.getString(R.string.cancel))
            .showIndicator(true)
            .showValue(true)
            .build()
            .show(object : ColorPickerPopup.ColorPickerObserver() {
                override fun onColorPicked(color: Int) {
                    PopupMenu(params.application, themesLayout).run {
                        menuInflater.inflate(R.menu.fragment_night_or_day, menu)
                        setOnMenuItemClickListener {
                            StorageUtil(params.application)
                                .storeCustomThemeColors(
                                    color to if (it.itemId == R.id.night_theme) 0 else 1
                                )

                            params.application.startActivity(
                                Intent(
                                    params.application,
                                    MainActivity::class.java
                                )
                            )

                            false
                        }
                    }
                }
            })
    }
}