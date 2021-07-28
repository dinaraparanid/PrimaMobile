package com.dinaraparanid.prima.utils.equalizer

import android.content.Context
import com.dinaraparanid.prima.utils.StorageUtil
import java.io.Serializable

/**
 * Equalizer params
 */

internal class EqualizerModel(context: Context) : Serializable {
    internal var isEqualizerEnabled = true
    internal var seekbarPos = StorageUtil(context).loadEqualizerSeekbarsPos() ?: IntArray(5)
    internal var presetPos = StorageUtil(context).loadPresetPos()
    internal var reverbPreset: Short = StorageUtil(context).loadReverbPreset()
    internal var bassStrength: Short = StorageUtil(context).loadBassStrength()
}