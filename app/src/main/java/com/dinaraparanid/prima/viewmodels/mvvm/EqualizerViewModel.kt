package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.media.PlaybackParams
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/**
 * MVVM View Model for [com.dinaraparanid.prima.fragments.EqualizerFragment]
 */

class EqualizerViewModel(private val activity: WeakReference<FragmentActivity>) : ViewModel() {
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

        val loader = StorageUtil(activity.unchecked)
        app.musicPlayer!!.playbackParams = PlaybackParams()
            .setPitch(if (isChecked) loader.loadPitch() else 1F)
            .setSpeed(if (isChecked) loader.loadSpeed() else 1F)
    }

    /** Changes bass amount */

    @JvmName("onControllerBassProgressChanged")
    internal fun onControllerBassProgressChanged(progress: Int) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) return
        EqualizerSettings.instance.bassStrength = (1000F / 19 * progress).toInt().toShort()
        (activity.unchecked.application as MainApplication).bassBoost!!.setStrength(EqualizerSettings.instance.bassStrength)
        EqualizerSettings.instance.equalizerModel!!.bassStrength =
            EqualizerSettings.instance.bassStrength

        if (Params.instance.saveEqualizerSettings)
            StorageUtil(activity.unchecked)
                .storeBassStrength(EqualizerSettings.instance.bassStrength)
    }
}