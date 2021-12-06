package com.dinaraparanid.prima.viewmodels.mvvm

import android.media.PlaybackParams
import android.os.Build
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import java.lang.ref.WeakReference

/**
 * MVVM View Model for [com.dinaraparanid.prima.fragments.EqualizerFragment]
 */

class EqualizerViewModel(private val activity: WeakReference<MainActivity>) : ViewModel() {
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
            val loader = StorageUtil.getInstanceSynchronized()
            app.musicPlayer!!.playbackParams = PlaybackParams()
                .setPitch(if (isChecked) loader.loadPitch() else 1F)
                .setSpeed(if (isChecked) loader.loadSpeed() else 1F)
        }
    }

    /** Changes bass amount */

    @JvmName("onControllerBassProgressChanged")
    internal fun onControllerBassProgressChanged(progress: Int) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) return
        EqualizerSettings.instance.bassStrength = (1000F / 19 * progress).toInt().toShort()
        (activity.unchecked.application as MainApplication).bassBoost!!.setStrength(EqualizerSettings.instance.bassStrength)
        EqualizerSettings.instance.equalizerModel!!.bassStrength =
            EqualizerSettings.instance.bassStrength

        if (Params.instance.saveEqualizerSettings) activity.unchecked.runOnIOThread {
            StorageUtil.getInstanceSynchronized().storeBassStrength(EqualizerSettings.instance.bassStrength)
        }
    }

    @JvmField
    internal val trackType = when {
        params.themeColor.second != -1 -> when (params.themeColor.second) {
            0 -> R.drawable.equalizer_track_horizontal_day
            else -> R.drawable.equalizer_track_horizontal_night
        }

        params.theme.isNight -> R.drawable.equalizer_track_horizontal_night
        else -> R.drawable.equalizer_track_horizontal_day
    }
}