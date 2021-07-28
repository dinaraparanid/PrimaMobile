package com.dinaraparanid.prima.utils.equalizer

import android.content.Context
import com.dinaraparanid.prima.utils.StorageUtil

/**
 * Equalizer settings
 */

internal class EqualizerSettings private constructor() {
    internal companion object {
        private var INSTANCE: EqualizerSettings? = null

        /**
         * Initializes settings only once
         */

        internal fun initialize(context: Context) {
            if (INSTANCE == null)
                INSTANCE = EqualizerSettings().apply {
                    StorageUtil(context).run {
                        loadEqualizerSeekbarsPos()?.let { seekbarPos = it }
                        presetPos = loadPresetPos()
                        reverbPreset = loadReverbPreset()
                        bassStrength = loadBassStrength()
                    }
                }
        }

        internal val instance: EqualizerSettings
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("EqualizerSettings is not initialized")
    }

    internal var isEqualizerEnabled = true
    internal var isEqualizerReloaded = true
    internal var seekbarPos = IntArray(5)
    internal var presetPos = 0
    internal var reverbPreset: Short = -1
    internal var bassStrength: Short = -1
    internal var equalizerModel: EqualizerModel? = null
    internal var ratio = 1.0
    internal var isEditing = false
}