package com.dinaraparanid.prima.utils.equalizer

import com.dinaraparanid.prima.utils.StorageUtil
import java.io.Serializable

/** Equalizer params */

internal class EqualizerModel private constructor() : Serializable {
    internal var isEqualizerEnabled = true
    internal var seekbarPos = IntArray(5)
    internal var presetPos = 0
    internal var reverbPreset: Short = 0
    internal var bassStrength: Short = 0

    internal companion object {
        /** Creates new instance of [EqualizerModel] with locks */
        @JvmStatic
        internal fun newInstance() = EqualizerModel().apply {
            seekbarPos = StorageUtil.instance.loadEqualizerSeekbarsPos() ?: IntArray(5)
            presetPos = StorageUtil.instance.loadPresetPos()
            reverbPreset = StorageUtil.instance.loadReverbPreset()
            bassStrength = StorageUtil.instance.loadBassStrength()
        }

        /** Creates new instance of [EqualizerModel] with locks */
        @JvmStatic
        internal suspend fun newInstanceLocking() = EqualizerModel().apply {
            seekbarPos = StorageUtil.getInstanceSynchronized().loadEqualizerSeekbarsPosLocking() ?: IntArray(5)
            presetPos = StorageUtil.getInstanceSynchronized().loadPresetPosLocking()
            reverbPreset = StorageUtil.getInstanceSynchronized().loadReverbPresetLocking()
            bassStrength = StorageUtil.getInstanceSynchronized().loadBassStrengthLocking()
        }
    }
}