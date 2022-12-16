package com.dinaraparanid.prima.mvvmp.repository

import android.annotation.SuppressLint
import android.view.View
import android.widget.PopupMenu
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainActivity.Companion.restart
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.view.dialogs.ColorPickerDialogFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.FontsFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.SettingsFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.ThemesFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractFragment
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.lang.ref.WeakReference

class SettingsRepository(
    fragment: SettingsFragment,
    private val params: Params,
    private val storageUtil: StorageUtil
) : Repository {
    private val fragmentRef by inject<WeakReference<SettingsFragment>> {
        parametersOf(fragment)
    }

    private inline val fragment
        get() = fragmentRef.unchecked

    private inline val context
        get() = fragment.requireContext()

    private inline val mainActivity
        get() = fragment.fragmentActivity

    // ------------------------------ Change Language ------------------------------

    private fun ChangeLangPopupMenu(anchor: View) = PopupMenu(context, anchor).apply {
        menuInflater.inflate(R.menu.menu_language, menu)
        setChangeLangCallbacks()
    }

    private fun PopupMenu.setChangeLangCallbacks() =
        setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_english -> params.changeLang(
                    context,
                    Params.Companion.Language.EN
                )

                R.id.nav_belarusian -> params.changeLang(
                    context,
                    Params.Companion.Language.BE
                )

                R.id.nav_russian -> params.changeLang(
                    context,
                    Params.Companion.Language.RU
                )

                R.id.nav_chinese -> params.changeLang(
                    context,
                    Params.Companion.Language.ZH
                )

                else -> throw IllegalArgumentException("Unknown language")
            }

            mainActivity.restart()
            true
        }

    /**
     * Changes language and restarts [MainActivity]
     * @param anchor Anchor view for this popup.
     * The popup will appear below the anchor
     * if there is room, or above it if there is not.
     */

    fun showChangeLangPopupMenu(anchor: View) =
        ChangeLangPopupMenu(anchor).show()

    // ------------------------------ Change Font ------------------------------

    fun showFontFragment() {
        mainActivity
            .supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                AbstractFragment.defaultInstance(
                    fragment.resources.getString(R.string.font),
                    FontsFragment::class
                )
            )
            .addToBackStack(null)
            .commit()
    }

    // ------------------------------ Change Text Color ------------------------------

    private val colorPickerObserver = object : ColorPickerDialogFragment.ColorPickerObserver() {
        @SuppressLint("SyntheticAccessor")
        override fun onColorPicked(color: Int) {
            params.fontColor = color
            storageUtil.storeFontColor(color)
        }
    }

    fun showColorPickerDialog() =
        get<ColorPickerDialogFragment> { parametersOf(fragment) }
            .show()

    // ------------------------------ Change Theme Color ------------------------------

    fun showThemesFragment() =
        mainActivity
            .supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                AbstractFragment.defaultInstance(
                    fragment.resources.getString(R.string.themes),
                    ThemesFragment::class
                )
            )
            .addToBackStack(null)
            .commit()

    // ------------------------------ Hide / Show Cover ------------------------------

    /**
     * Shows or hides track's cover on playback panel
     * @param isShown is cover shown or hidden
     */

    fun setPlayingTrackCoverShown(isShown: Boolean) {
        storageUtil.storeHideCover(isShown)
        params.isCoverHidden = isShown
    }

    // ------------------------------ Display Cover ------------------------------

    /**
     * Displays covers or shows only the default one
     * @param isShown shows albums' covers or the default one
     */

    fun setCoversDisplayed(isShown: Boolean) {
        storageUtil.storeDisplayCovers(isShown)
        params.isCoversDisplayed = isShown
    }

    // ------------------------------ Rotate Cover ------------------------------

    /**
     * Rotates track's cover on small playback panel
     * @param isRotating is cover rotated
     */

    fun setPlayingTrackCoverRotate(isRotating: Boolean) {
        storageUtil.storeRotateCover(isRotating)
        params.isCoverRotating = isRotating
        mainActivity.startOrStopCoverRotating(isRotating)
    }

    // ------------------------------ Cover Rounding ------------------------------

    /**
     * Add or removes rounding of playlists' images
     * @param areRounding add or remove rounding
     */

    fun setCoversRounded(areRounding: Boolean) {
        storageUtil.storeCoversRounded(areRounding)
        params.areCoversRounded = areRounding
    }
}