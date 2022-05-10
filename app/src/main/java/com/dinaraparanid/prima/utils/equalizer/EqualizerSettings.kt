package com.dinaraparanid.prima.utils.equalizer

import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/** Equalizer settings */

internal class EqualizerSettings private constructor() : CoroutineScope by MainScope() {
    internal companion object {
        private var INSTANCE: EqualizerSettings? = null

        /** Initializes settings only once */

        internal fun initialize() {
            if (INSTANCE == null)
                INSTANCE = EqualizerSettings().apply {
                    StorageUtil.instance.run {
                        launch {
                            loadEqualizerSeekbarsPosLocking()?.let { seekbarPos = it }
                            presetPos = loadPresetPosLocking()
                            reverbPreset = loadReverbPresetLocking()
                            bassStrength = loadBassStrengthLocking()
                        }
                    }
                }
        }

        /** Gets instance of [EqualizerSettings] */

        @JvmStatic
        internal val instance: EqualizerSettings
            @JvmName("getInstance")
            get() = INSTANCE
                ?: throw UninitializedPropertyAccessException("EqualizerSettings is not initialized")
    }

    internal var isEqualizerEnabled = false
        @JvmName("isEqualizerEnabled") get

    internal var isEqualizerReloaded = true
    internal var seekbarPos = IntArray(5)
    internal var presetPos = 0
    internal var reverbPreset: Short = -1
    internal var bassStrength: Short = -1
    internal var equalizerModel: EqualizerModel? = null
    internal var ratio = 1.0
    internal var isEditing = false
}