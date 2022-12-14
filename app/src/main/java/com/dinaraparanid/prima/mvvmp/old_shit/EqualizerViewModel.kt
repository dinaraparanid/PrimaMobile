package com.dinaraparanid.prima.mvvmp.old_shit

import android.media.PlaybackParams
import android.os.Build
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.getBetween
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.playing_panel_fragments.EqualizerFragment]
 */

class EqualizerViewModel(private val activity: WeakReference<MainActivity>) : BasePresenter() {
    /** Clears equalizer fragment */

    @JvmName("onBackButtonPressed")
    internal fun onBackButtonPressed() = activity.unchecked.supportFragmentManager.popBackStack()

    /** Enables or disables equalizer */

    @JvmName("onSwitchCheckedChange")
    internal fun onSwitchCheckedChange(isChecked: Boolean) {
        val app = activity.unchecked.application as MainApplication
        app.equalizer.enabled = isChecked
        app.bassBoost?.enabled = isChecked
        app.presetReverb?.enabled = isChecked
        EqualizerSettings.instance.isEqualizerEnabled = isChecked
        EqualizerSettings.instance.equalizerModel!!.isEqualizerEnabled = isChecked

        activity.unchecked.runOnIOThread {
            val loader = StorageUtil.getInstanceAsyncSynchronized()
            app.musicPlayer!!.playbackParams = PlaybackParams()
                .setPitch(if (isChecked) loader.loadPitchAsyncLocking() else 1F)
                .setSpeed(if (isChecked) loader.loadSpeedAsyncLocking() else 1F)
        }
    }

    /** Changes bass amount */

    internal fun onControllerBassProgressChanged(progress: Int) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            return

        EqualizerSettings.instance.bassStrength =
            (1000F / 20 * progress).toInt().toShort().getBetween(0, 1000)

        (activity.unchecked.application as MainApplication).bassBoost!!.setStrength(
            EqualizerSettings.instance.bassStrength
        )

        EqualizerSettings.instance.equalizerModel!!.bassStrength =
            EqualizerSettings.instance.bassStrength

        if (Params.instance.isSavingEqualizerSettings) activity.unchecked.runOnIOThread {
            StorageUtil
                .getInstanceAsyncSynchronized()
                .storeBassStrengthLocking(EqualizerSettings.instance.bassStrength)
        }
    }

    @JvmField
    internal val trackType = when {
        params.secondaryColor != -1 -> when (params.secondaryColor) {
            0 -> R.drawable.equalizer_track_horizontal_day_transparent
            else -> R.drawable.equalizer_track_horizontal_night_transparent
        }

        params.theme.isNight -> R.drawable.equalizer_track_horizontal_night_transparent
        else -> R.drawable.equalizer_track_horizontal_day_transparent
    }
}